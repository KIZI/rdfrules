package com.github.propi.rdfrules.ruleset

import com.github.propi.rdfrules.algorithm.Clustering
import com.github.propi.rdfrules.algorithm.amie.RuleCounting._
import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting
import com.github.propi.rdfrules.data.ops.{Cacheable, Debugable, Transformable}
import com.github.propi.rdfrules.index.{AutoIndex, Index, TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.prediction.{InstantiatedRuleset, Instantiation, PredictedResult, PredictedTriples, Prediction}
import com.github.propi.rdfrules.rule.PatternMatcher.Aliases
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.rule.RulePatternMatcher._
import com.github.propi.rdfrules.rule.{Measure, PatternMatcher, ResolvedRule, Rule, RulePattern}
import com.github.propi.rdfrules.ruleset.ops.{Sortable, Treeable}
import com.github.propi.rdfrules.serialization.RuleSerialization._
import com.github.propi.rdfrules.utils.TypedKeyMap.Key
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}
import com.github.propi.rdfrules.utils.{Debugger, ForEach, TypedKeyMap}

import java.io._
import scala.collection.mutable
import scala.math.Ordering.Implicits.seqOrdering

/**
  * Created by Vaclav Zeman on 6. 10. 2017.
  */
class Ruleset private(val rules: ForEach[FinalRule], val index: Index, val parallelism: Int)
  extends Transformable[FinalRule, Ruleset]
    with Cacheable[FinalRule, Ruleset]
    with Sortable[FinalRule, Ruleset]
    with Debugable[FinalRule, Ruleset]
    with Treeable {

  self =>

  protected def coll: ForEach[FinalRule] = rules

  protected def transform(col: ForEach[FinalRule]): Ruleset = new Ruleset(col, index, parallelism)

  protected def cachedTransform(col: ForEach[FinalRule]): Ruleset = new Ruleset(col, index, parallelism)

  protected val ordering: Ordering[FinalRule] = implicitly[Ordering[FinalRule]]
  protected val serializer: Serializer[FinalRule] = Serializer.by[FinalRule, ResolvedRule](ResolvedRule(_)(index.tripleItemMap))
  protected val deserializer: Deserializer[FinalRule] = Deserializer.by[ResolvedRule, FinalRule](_.toRule(index.tripleItemMap))
  protected val serializationSize: SerializationSize[FinalRule] = SerializationSize.by[FinalRule, ResolvedRule]
  protected val dataLoadingText: String = "Ruleset loading"

  def withIndex(index: Index): Ruleset = new Ruleset(rules, index, parallelism)

  def filter(pattern: RulePattern, patterns: RulePattern*): Ruleset = transform((f: FinalRule => Unit) => {
    implicit val mapper: TripleItemIndex = index.tripleItemMap
    implicit val thi: TripleIndex.Builder[Int] = index
    val rulePatternMatcher = implicitly[PatternMatcher[Rule, RulePattern.Mapped]]
    val mappedPatterns = (pattern +: patterns).map(_.withOrderless().mapped)
    rules.filter(rule => mappedPatterns.exists(rulePattern => rulePatternMatcher.matchPattern(rule, rulePattern)(Aliases.empty).isDefined)).foreach(f)
  })

  def filterResolved(f: ResolvedRule => Boolean): Ruleset = transform((f2: FinalRule => Unit) => {
    implicit val mapper: TripleItemIndex = index.tripleItemMap
    rules.filter(x => f(x)).foreach(f2)
  })

  def sortBy(measure: Key[Measure], measures: Key[Measure]*): Ruleset = sortBy { rule =>
    rule.measures(measure) +: measures.map(rule.measures(_))
  }

  def sortByResolved[A](f: ResolvedRule => A)(implicit ord: Ordering[A]): Ruleset = {
    implicit val mapper: TripleItemIndex = index.tripleItemMap
    sortBy(x => f(x))
  }

  def sortByRuleLength(measures: Key[Measure]*): Ruleset = sortBy { rule =>
    (rule.ruleLength, measures.map(rule.measures(_)))
  }

  def resolvedRules: ForEach[ResolvedRule] = new ForEach[ResolvedRule] {
    def foreach(f: ResolvedRule => Unit): Unit = {
      implicit val mapper: TripleItemIndex = index.tripleItemMap
      rules.foreach(x => f(x))
    }

    override def knownSize: Int = rules.knownSize
  }

  def foreach(f: ResolvedRule => Unit): Unit = resolvedRules.foreach(f)

  def +(ruleset: Ruleset): Ruleset = transform(rules.concat(ruleset.rules))

  def headResolved: ResolvedRule = resolvedRules.head

  def headResolvedOption: Option[ResolvedRule] = resolvedRules.headOption

  def findResolved(f: ResolvedRule => Boolean): Option[ResolvedRule] = resolvedRules.find(f)

  /**
    * Prune rules with CBA strategy
    *
    * @param onlyExistingTriples      if true the common CBA strategy will be used. That means we take only such predicted triples (of the rules),
    *                                 which are contained in the input dataset. This strategy takes maximally as much memory as the number of triples
    *                                 in the input dataset. If false we take all predicted triples (including triples which are not contained in the
    *                                 input dataset and are newly generated). For deduplication a HashSet is used and therefore the memory may increase
    *                                 unexpectedly because we need to save all unique generated triples into memory.
    * @param onlyFunctionalProperties if true the predicted triples are deduplicated by (subject, predicate). E.g. if some triple (A B C) is generated
    *                                 for some rule then next generated triples with form (A B *) are skipped. We expect only functional properties;
    *                                 it means the tuple (subject, predicate) can have only one object. If you expect non function properties, set
    *                                 this parameter to false.
    * @return pruned ruleset
    */
  def pruned(onlyExistingTriples: Boolean = true, onlyFunctionalProperties: Boolean = true, injectiveMapping: Boolean = true)(implicit debugger: Debugger): Ruleset = {
    transform((f: FinalRule => Unit) => {
      implicit val mapper: TripleItemIndex = index.tripleItemMap
      val predictedResults: Set[PredictedResult] = if (onlyExistingTriples) Set(PredictedResult.Positive) else Set.empty
      val predictionResult = if (onlyFunctionalProperties) predict(predictedResults, injectiveMapping).onlyFunctionalPredictions else predict(predictedResults, injectiveMapping).distinctPredictions
      val hashSet = collection.mutable.LinkedHashSet.empty[FinalRule]
      for (rule <- predictionResult.triples.map(_.rule)) {
        hashSet += rule
      }
      hashSet.foreach(f)
    })
  }

  def withoutQuasiBinding(injectiveMapping: Boolean = true)(implicit debugger: Debugger): Ruleset = {
    implicit val ti: TripleIndex[Int] = index.tripleMap
    implicit val tii: TripleItemIndex = index.tripleItemMap
    transform(rules.parMap(parallelism)(x => Option(x).filter(!_.hasQuasiBinding(injectiveMapping)))
      .withDebugger("Quasi binding filtering")
      .filter(_.isDefined)
      .map(_.get))
  }

  def graphAwareRules: Ruleset = {
    implicit val ad: Int => (Debugger.ActionDebugger => Unit) => Unit = size => f => Debugger.EmptyDebugger.debug("", size)(f)
    transform(new ForEach[FinalRule] {
      def foreach(f: FinalRule => Unit): Unit = {
        implicit val tripleMap: TripleIndex[Int] = index.tripleMap
        rules.map(rule => Rule(rule.head.toGraphAwareAtom, rule.body.map(_.toGraphAwareAtom))(rule.measures)).foreach(f)
      }

      override def knownSize: Int = rules.knownSize
    })
  }

  def computeConfidence(minConfidence: Double, injectiveMapping: Boolean = true, topK: Int = 0)(implicit debugger: Debugger): Ruleset = {
    @volatile var threshold = minConfidence
    implicit val ti: TripleIndex[Int] = index.tripleMap
    implicit val tii: TripleItemIndex = index.tripleItemMap

    val rulesWithConfidence = rules.parMap(parallelism) { rule =>
      rule.withConfidence(threshold, injectiveMapping)
    }.filter(_.measures.get[Measure.Confidence].exists(_.value >= minConfidence)).withDebugger("Confidence computing")

    val resColl = if (topK > 0) {
      //if we use topK approach then the final ruleset will have size lower than or equals to the original size
      //therefore we shrink the original ruleset
      //first we need to define rule ordering for priority queue
      implicit val ord: Ordering[FinalRule] = Ordering.by[FinalRule, (Double, Double)](x => x.measures.get[Measure.Confidence].map(_.value).getOrElse(0.0) -> x.measures.apply[Measure.HeadCoverage].value).reverse
      rulesWithConfidence.topK(topK)(rule => threshold = rule.measures.apply[Measure.Confidence].value)
    } else {
      rulesWithConfidence
    }
    transform(resColl)
  }

  def computePcaConfidence(minPcaConfidence: Double, injectiveMapping: Boolean = true, topK: Int = 0)(implicit debugger: Debugger): Ruleset = {
    @volatile var threshold = minPcaConfidence
    implicit val ti: TripleIndex[Int] = index.tripleMap
    implicit val tii: TripleItemIndex = index.tripleItemMap

    val rulesWithConfidence = rules.parMap(parallelism) { rule =>
      rule.withPcaConfidence(threshold, injectiveMapping)
    }.filter(_.measures.get[Measure.PcaConfidence].exists(_.value >= minPcaConfidence)).withDebugger("Confidence computing")

    val resColl = if (topK > 0) {
      //if we use topK approach then the final ruleset will have size lower than or equals to the original size
      //therefore we shrink the original ruleset
      //first we need to define rule ordering for priority queue
      implicit val ord: Ordering[FinalRule] = Ordering.by[FinalRule, (Double, Double)](x => x.measures.get[Measure.PcaConfidence].map(_.value).getOrElse(0.0) -> x.measures.apply[Measure.HeadCoverage].value).reverse
      rulesWithConfidence.topK(topK)(rule => threshold = rule.measures.apply[Measure.PcaConfidence].value)
    } else {
      rulesWithConfidence
    }
    transform(resColl)
  }

  def computeLift(minConfidence: Double = 0.5, injectiveMapping: Boolean = true)(implicit debugger: Debugger): Ruleset = {
    implicit val ti: TripleIndex[Int] = index.tripleMap
    implicit val tii: TripleItemIndex = index.tripleItemMap
    val resColl = rules.parMap(parallelism) { rule =>
      Function.chain[FinalRule](List(
        rule => if (rule.measures.exists[Measure.Confidence]) rule else rule.withConfidence(minConfidence, injectiveMapping),
        rule => if (rule.measures.exists[Measure.HeadConfidence]) rule else rule.withHeadConfidence,
        rule => rule.withLift
      ))(rule)
    }.withDebugger("Lift computing")
    transform(resColl)
  }

  def instantiate(predictionResults: Set[PredictedResult] = Set.empty, injectiveMapping: Boolean = true): InstantiatedRuleset = {
    InstantiatedRuleset(index, Instantiation(rules, index, predictionResults, injectiveMapping))
  }

  def predict(predictedResults: Set[PredictedResult] = Set.empty, injectiveMapping: Boolean = true)(implicit debugger: Debugger): PredictedTriples = {
    PredictedTriples(index, Prediction(rules.withDebugger("Predicted rules"), index, predictedResults, injectiveMapping))
  }

  def makeClusters(clustering: Clustering[FinalRule]): Ruleset = transform((f: FinalRule => Unit) => clustering.clusters(rules.toIndexedSeq).view.zipWithIndex.flatMap { case (cluster, index) =>
    cluster.map(x => x.withMeasures(TypedKeyMap(Measure.Cluster(index)) ++= x.measures))
  }.foreach(f))

  def findSimilar(rule: ResolvedRule, k: Int, dissimilar: Boolean = false)(implicit simf: SimilarityCounting[FinalRule]): Ruleset = if (k < 1) {
    findSimilar(rule, 1, dissimilar)
  } else {
    transform((f: FinalRule => Unit) => {
      implicit val mapper: TripleItemIndex = index.tripleItemMap
      val ruleSimple = rule.toRule
      val ordering = Ordering.by[(Double, FinalRule), Double](_._1)
      val queue = mutable.PriorityQueue.empty(if (dissimilar) ordering else ordering.reverse)
      for (rule2 <- rules if rule2 != ruleSimple) {
        val sim = simf(ruleSimple, rule2)
        if (queue.size < k) {
          queue.enqueue(sim -> rule2)
        } else if ((!dissimilar && sim > queue.head._1) || (dissimilar && sim < queue.head._1)) {
          queue.dequeue()
          queue.enqueue(sim -> rule2)
        }
      }
      queue.dequeueAll[(Double, FinalRule)].reverseIterator.map(_._2).foreach(f)
    })
  }

  def findDissimilar(rule: ResolvedRule, k: Int)(implicit simf: SimilarityCounting[FinalRule]): Ruleset = findSimilar(rule, k, true)

  def `export`(os: => OutputStream)(implicit writer: RulesetWriter): Unit = writer.writeToOutputStream(this, os)

  def `export`(file: File)(implicit writer: RulesetWriter): Unit = {
    val newWriter = if (writer == RulesetWriter.NoWriter) RulesetWriter(file) else writer
    `export`(new FileOutputStream(file))(newWriter)
  }

  def `export`(file: String)(implicit writer: RulesetWriter): Unit = `export`(new File(file))

  /**
    * Set number of workers for parallel tasks (confidences computing)
    * The parallelism should be equal to or lower than the max thread pool size of the execution context
    *
    * @param parallelism number of workers
    * @return
    */
  def setParallelism(parallelism: Int): Ruleset = {
    val normParallelism = if (parallelism < 1 || parallelism > Runtime.getRuntime.availableProcessors()) {
      Runtime.getRuntime.availableProcessors()
    } else {
      parallelism
    }
    new Ruleset(rules, index, normParallelism)
  }

}

object Ruleset {

  private def resolvedReader(file: File)(implicit reader: RulesetReader): RulesetReader = if (reader == RulesetReader.NoReader) RulesetReader(file) else reader

  def apply(index: Index, rules: ForEach[FinalRule]): Ruleset = new Ruleset(rules, index, Runtime.getRuntime.availableProcessors())

  def apply(index: Index, rules: ForEach[ResolvedRule])(implicit i1: DummyImplicit): Ruleset = apply(index, rules.flatMap(_.toRuleOpt(index.tripleItemMap)))

  def apply(index: Index, file: File)(implicit reader: RulesetReader): Ruleset = apply(index, resolvedReader(file).fromFile(file))

  def apply(index: Index, file: String)(implicit reader: RulesetReader): Ruleset = apply(index, new File(file))

  def apply(index: Index, is: => InputStream)(implicit reader: RulesetReader): Ruleset = apply(index, reader.fromInputStream(is))

  def apply(rules: ForEach[ResolvedRule]): Ruleset = apply(AutoIndex(), rules)

  def apply(file: File)(implicit reader: RulesetReader): Ruleset = apply(resolvedReader(file).fromFile(file))

  def apply(file: String)(implicit reader: RulesetReader): Ruleset = apply(new File(file))

  def apply(is: => InputStream)(implicit reader: RulesetReader): Ruleset = apply(reader.fromInputStream(is))

  def fromCache(index: Index, is: => InputStream): Ruleset = apply(
    index,
    (f: FinalRule => Unit) => Deserializer.deserializeFromInputStream[ResolvedRule, Unit](is) { reader =>
      Iterator.continually(reader.read()).takeWhile(_.isDefined).map(_.get).flatMap(_.toRuleOpt(index.tripleItemMap)).foreach(f)
    }
  )

  def fromCache(index: Index, file: File): Ruleset = fromCache(index, new FileInputStream(file))

  def fromCache(index: Index, file: String): Ruleset = fromCache(index, new File(file))

  def fromCache(is: => InputStream): Ruleset = fromCache(AutoIndex(), is)

  def fromCache(file: File): Ruleset = fromCache(new FileInputStream(file))

  def fromCache(file: String): Ruleset = fromCache(new File(file))

}
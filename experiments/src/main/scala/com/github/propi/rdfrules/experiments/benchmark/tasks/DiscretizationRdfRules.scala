package com.github.propi.rdfrules.experiments.benchmark.tasks

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.experiments.IndexOps._
import com.github.propi.rdfrules.experiments.benchmark.{DefaultMiningSettings, Task, TaskPostProcessor, TaskPreProcessor}
import com.github.propi.rdfrules.index.ops.CollectionBuilders._
import com.github.propi.rdfrules.index.{IndexPart, TripleHashIndex, TripleItemHashIndex}
import com.github.propi.rdfrules.rule.Threshold
import com.github.propi.rdfrules.utils.{Debugger, ForEach}

/**
  * Created by Vaclav Zeman on 10. 4. 2020.
  */
class DiscretizationRdfRules(val name: String, override val minHeadCoverage: Double = DefaultMiningSettings.minHeadCoverage)
                            (implicit val debugger: Debugger) extends Task[IndexPart, IndexPart, IndexPart, IndexPart] with TaskPreProcessor[IndexPart, IndexPart] with TaskPostProcessor[IndexPart, IndexPart] with DefaultMiningSettings {

  protected val minSupportLowerBoundOn = true
  protected val minSupportUpperBoundOn = true

  protected def postProcess(result: IndexPart): IndexPart = result

  protected def preProcess(input: IndexPart): IndexPart = input

  protected def taskBody(input: IndexPart): IndexPart = {
    val minSupportLower = Function.chain[Map[Int, Int]](List(
      x => if (minSupportLowerBoundOn) x else x.map {
        case (p, _) => p -> 1
      }
    ))(input.useRichOps(_.getMinSupportLower(Threshold.MinHeadSize(100), Threshold.MinHeadCoverage(minHeadCoverage), maxRuleLength)))
    val minSupportUpper = Function.chain[Map[Int, Int]](List(
      x => if (minSupportUpperBoundOn) x else x.map {
        case (p, _) => p -> Int.MaxValue
      }
    ))(input.useRichOps(_.getMinSupportUpper(Threshold.MinHeadSize(100), Threshold.MinHeadCoverage(minHeadCoverage), maxRuleLength)))
    val predicates = input.useRichOps(_.getNumericPredicates(Iterator.empty, minSupportLower).toList)
    val trees = input.useRichOps(_.getDiscretizedTrees(predicates.iterator.map(_._1), minSupportLower, 2).toList)
    input.useRichOps { x =>
      val newQuads = ForEach.from(trees).flatMap(y => x.discretizedTreeQuads(y._1.asInstanceOf[TripleItem.Uri], minSupportUpper(input.tripleItemMap.getIndex(y._1)), y._2))
      val (tihi, thi) = TripleItemHashIndex.mapQuads(input.toDataset.quads.concat(newQuads))(TripleHashIndex(_))
      IndexPart(thi, tihi)
    }
  }
}
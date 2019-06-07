package com.github.propi.rdfrules.algorithm.amie

import java.util.concurrent.ForkJoinPool

import com.github.propi.rdfrules.index.TripleHashIndex
import com.github.propi.rdfrules.rule.ExtendedRule.DanglingRule
import com.github.propi.rdfrules.rule.{Atom, AtomPatternMatcher, ExtendedRule, Measure, RulePattern, Threshold}
import com.github.propi.rdfrules.utils.TypedKeyMap

import scala.collection.parallel.ForkJoinTaskSupport

/**
  * Created by Vaclav Zeman on 3. 6. 2019.
  */
trait HeadsFetcher extends AtomCounting {

  implicit val tripleIndex: TripleHashIndex
  implicit val settings: RuleRefinement.Settings
  implicit val forAtomMatcher: AtomPatternMatcher[Atom]

  val patterns: List[RulePattern]
  val thresholds: TypedKeyMap.Immutable[Threshold]

  private lazy val minSupport: Int = thresholds.apply[Threshold.MinHeadSize].value

  /**
    * Get all possible heads from triple index
    * All heads are filtered by constraints, patterns and minimal support
    *
    * @return atoms in dangling rule form - DanglingRule(no body, one head, one or two variables in head)
    */
  def getHeads: Vector[DanglingRule] = {
    //enumerate all possible head atoms with variables and instances
    //all unsatisfied predicates are filtered by constraints
    val logicalHeads = tripleIndex.predicates.keysIterator.filter(settings.isValidPredicate).map(Atom(Atom.Variable(0), _, Atom.Variable(1))).toVector.par
    logicalHeads.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(settings.parallelism))
    logicalHeads.flatMap { logicalHead =>
      val possibleAtoms = if (settings.isWithInstances) {
        //if instantiated atoms are allowed then we specify variables by constants
        //only one variable may be replaced by a constant
        //result is original variable atom + instantied atoms with constants in subject + instantied atoms with constants in object
        //we do not instantient subject variable if onlyObjectInstances is true
        val it1 = if (settings.onlyObjectInstances) {
          Iterator.empty
        } else {
          specifySubject(logicalHead).map(_.transform(`object` = Atom.Variable(0)))
        }
        val it2 = specifyObject(logicalHead)
        Iterator(logicalHead) ++ it1 ++ it2
      } else {
        Iterator(logicalHead)
      }
      //filter all atoms by patterns and join all valid patterns to the atom
      possibleAtoms.flatMap { atom =>
        val validPatterns = settings.patterns.filter(rp => rp.consequent.forall(atomPattern => forAtomMatcher.matchPattern(atom, atomPattern)))
        if (patterns.isEmpty || validPatterns.nonEmpty) {
          Iterator(atom -> validPatterns)
        } else {
          Iterator.empty
        }
      }.flatMap { case (atom, patterns) =>
        //convert all atoms to rules and filter it by min support
        val tm = tripleIndex.predicates(atom.predicate)
        val danglingRule = Some(atom.subject, atom.`object`).collect {
          case (v1: Atom.Variable, v2: Atom.Variable) => (ExtendedRule.TwoDanglings(v1, v2, Nil), tm.size, tm.size)
          case (Atom.Constant(c), v1: Atom.Variable) => (ExtendedRule.OneDangling(v1, Nil), tm.size, tm.subjects.get(c).map(_.size).getOrElse(0))
          case (v1: Atom.Variable, Atom.Constant(c)) => (ExtendedRule.OneDangling(v1, Nil), tm.size, tm.objects.get(c).map(_.size).getOrElse(0))
        }.filter(x => x._3 >= math.max(minSupport, settings.minHeadCoverage * x._2)).map(x =>
          DanglingRule(Vector.empty, atom)(
            TypedKeyMap(Measure.HeadSize(x._2), Measure.Support(x._3), Measure.HeadCoverage(x._3.toDouble / x._2)),
            patterns,
            x._1,
            x._1.danglings.max,
            getAtomTriples(atom).toIndexedSeq
          )
        )
        danglingRule
      }
    }.seq
  }

}
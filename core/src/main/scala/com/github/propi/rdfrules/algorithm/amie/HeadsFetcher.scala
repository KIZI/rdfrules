package com.github.propi.rdfrules.algorithm.amie

import java.util.concurrent.{ConcurrentLinkedQueue, ForkJoinPool}

import com.github.propi.rdfrules.data.TriplePosition
import com.github.propi.rdfrules.index.TripleIndex
import com.github.propi.rdfrules.rule.ExtendedRule.DanglingRule
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule.{Atom, ExtendedRule, MappedAtomPatternMatcher}
import com.github.propi.rdfrules.utils.Debugger.ActionDebugger
import com.github.propi.rdfrules.utils.{Debugger, UniqueQueue}

import scala.collection.parallel.ForkJoinTaskSupport

/**
  * Created by Vaclav Zeman on 3. 6. 2019.
  */
trait HeadsFetcher extends AtomCounting {

  implicit val tripleIndex: TripleIndex[Int]
  implicit val settings: RuleRefinement.Settings
  implicit val forAtomMatcher: MappedAtomPatternMatcher[Atom]

  private def logicalHeadToRules(logicalHead: Atom)(implicit actionDebugger: ActionDebugger): Iterator[DanglingRule] = {
    val possibleAtoms = if (settings.isWithInstances) {
      //if instantiated atoms are allowed then we specify variables by constants
      //only one variable may be replaced by a constant
      //result is original variable atom + instantied atoms with constants in subject + instantied atoms with constants in object
      //we do not instantient subject variable if onlyObjectInstances is true
      val it = settings.constantsPosition match {
        case Some(ConstantsPosition.LeastFunctionalVariable) => tripleIndex.predicates(logicalHead.predicate).leastFunctionalVariable match {
          case TriplePosition.Subject => specifySubject(logicalHead).map(_.transform(`object` = Atom.Variable(0)))
          case TriplePosition.Object => specifyObject(logicalHead)
        }
        case Some(ConstantsPosition.Object) => specifyObject(logicalHead)
        case Some(ConstantsPosition.Subject) => specifySubject(logicalHead).map(_.transform(`object` = Atom.Variable(0)))
        case _ => specifySubject(logicalHead).map(_.transform(`object` = Atom.Variable(0))) ++ specifyObject(logicalHead)
      }
      Iterator(logicalHead) ++ it.filter(settings.test)
    } else {
      Iterator(logicalHead)
    }
    //filter all atoms by patterns and join all valid patterns to the atom
    possibleAtoms.filter { atom =>
      settings.patterns.isEmpty || settings.patterns.exists(rp => rp.head.forall(atomPattern => forAtomMatcher.matchPattern(atom, atomPattern)))
    }.flatMap { atom =>
      //convert all atoms to rules and filter it by min support
      val tm = tripleIndex.predicates(atom.predicate)
      actionDebugger.done()
      val danglingRule = Some(atom.subject, atom.`object`).collect {
        case (v1: Atom.Variable, v2: Atom.Variable) => (ExtendedRule.TwoDanglings(v1, v2, Nil), tm.size, tm.size)
        case (Atom.Constant(c), v1: Atom.Variable) => (ExtendedRule.OneDangling(v1, Nil), tm.size, tm.subjects.get(c).map(_.size).getOrElse(0))
        case (v1: Atom.Variable, Atom.Constant(c)) => (ExtendedRule.OneDangling(v1, Nil), tm.size, tm.objects.get(c).map(_.size).getOrElse(0))
      }.filter(x => x._2 >= settings.minHeadSize && x._3 >= math.max(settings.minSupport, settings.minHeadCoverage * x._2)).map(x =>
        DanglingRule(Vector.empty, atom)(
          x._3,
          x._2,
          x._1,
          x._1.danglings.max
        )
      )
      danglingRule
    }
  }

  /**
    * Get all possible heads from triple index
    * All heads are filtered by constraints, patterns and minimal support
    *
    * @return atoms in dangling rule form - DanglingRule(no body, one head, one or two variables in head)
    */
  def getHeads(implicit debugger: Debugger): UniqueQueue[ExtendedRule] = {
    //enumerate all possible head atoms with variables and instances
    //all unsatisfied predicates are filtered by constraints
    val logicalHeads = tripleIndex.predicates.iterator.filter(settings.isValidPredicate).map(Atom(Atom.Variable(0), _, Atom.Variable(1))).filter(settings.test).toVector.par
    logicalHeads.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(settings.parallelism))
    debugger.debug("Heads mining") { implicit ad =>
      val result = new ConcurrentLinkedQueue[ExtendedRule]()
      logicalHeads.foreach { head =>
        logicalHeadToRules(head).foreach(result.add)
      }
      result
    }
  }

}
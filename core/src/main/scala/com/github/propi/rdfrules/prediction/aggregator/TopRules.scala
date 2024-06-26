package com.github.propi.rdfrules.prediction.aggregator

import com.github.propi.rdfrules.prediction.PredictedTriplesAggregator.RulesFactory
import com.github.propi.rdfrules.rule.{DefaultConfidence, Rule}
import com.github.propi.rdfrules.utils.TopKQueue
import com.github.propi.rdfrules.rule.Measure.ConfidenceFirstOrdering._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class TopRules private(topK: Int)(implicit defaultConfidence: DefaultConfidence) extends RulesFactory {
  def newBuilder: mutable.Builder[Rule.FinalRule, Iterable[Rule.FinalRule]] = {
    val queue = new TopKQueue[Rule.FinalRule](topK, false)(implicitly[Ordering[Rule.FinalRule]])
    new mutable.Builder[Rule.FinalRule, Iterable[Rule.FinalRule]] {
      def clear(): Unit = queue.clear()

      def result(): Iterable[Rule.FinalRule] = {
        val res = ListBuffer.empty[Rule.FinalRule]
        queue.dequeueAll.foreach(res.prepend)
        res
      }

      def addOne(elem: Rule.FinalRule): this.type = {
        queue.enqueue(elem)
        this
      }
    }
  }
}

object TopRules {
  def apply(topK: Int = -1)(implicit defaultConfidence: DefaultConfidence = DefaultConfidence()): TopRules = new TopRules(topK)(defaultConfidence)
}

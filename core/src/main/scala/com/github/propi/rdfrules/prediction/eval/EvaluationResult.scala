package com.github.propi.rdfrules.prediction.eval

sealed trait EvaluationResult

object EvaluationResult {

  case class Stats(predictionTasks: Int) extends EvaluationResult

  case class Ranking(hitsK: Seq[HitsK], mr: Double, mrr: Double, total: Int, totalCorrect: Int) extends EvaluationResult

  case class Completeness(tp: Int, fp: Int, fn: Int, tn: Int) extends EvaluationResult {
    def accuracy: Double = {
      val d = tp + fp + fn + tn
      if (d > 0) (tp + tn).toDouble / d else 0
    }

    def precision: Double = {
      val d = tp + fp
      if (d > 0) tp.toDouble / d else 0
    }

    def recall: Double = {
      val d = tp + fn
      if (d > 0) tp.toDouble / d else 0
    }

    def fscore: Double = {
      val p = precision
      val r = recall
      if (p + r > 0) (2 * p * r) / (p + r) else 0
    }

    override def toString: String = s"accuracy: $accuracy, precision: $precision, recall: $recall, f-measure: $fscore (TP: $tp, FP: $fp, FN: $fn, TN: $tn)"
  }

}
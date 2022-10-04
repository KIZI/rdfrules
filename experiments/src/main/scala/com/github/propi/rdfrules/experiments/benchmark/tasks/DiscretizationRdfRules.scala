package com.github.propi.rdfrules.experiments.benchmark.tasks

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.experiments.IndexOps._
import com.github.propi.rdfrules.experiments.benchmark.{DefaultMiningSettings, Task, TaskPostProcessor, TaskPreProcessor}
import com.github.propi.rdfrules.index.ops.CollectionBuilders
import com.github.propi.rdfrules.index.{Index, TripleHashIndex, TripleItemHashIndex}
import com.github.propi.rdfrules.rule.Threshold
import com.github.propi.rdfrules.utils.{Debugger, ForEach}

/**
  * Created by Vaclav Zeman on 10. 4. 2020.
  */
class DiscretizationRdfRules(val name: String, override val minHeadCoverage: Double = DefaultMiningSettings.minHeadCoverage)
                            (implicit val debugger: Debugger) extends Task[Index, Index, Index, Index] with TaskPreProcessor[Index, Index] with TaskPostProcessor[Index, Index] with DefaultMiningSettings with CollectionBuilders {

  protected def postProcess(result: Index): Index = result

  protected def preProcess(input: Index): Index = input

  protected def taskBody(input: Index): Index = {
    val minSupportLower = input.useRichOps(_.getMinSupportLower(Threshold.MinHeadSize(100), Threshold.MinHeadCoverage(minHeadCoverage)))
    val minSupportUpper = input.useRichOps(_.getMinSupportUpper(Threshold.MinHeadSize(100), Threshold.MinHeadCoverage(minHeadCoverage)))
    val predicates = input.useRichOps(_.getNumericPredicates(Iterator.empty, minSupportLower).toList)
    val trees = input.useRichOps(_.getDiscretizedTrees(predicates.iterator.map(_._1), minSupportLower, 2).toList)
    input.useRichOps { x =>
      val newQuads = ForEach.from(trees).flatMap(y => x.discretizedTreeQuads(y._1.asInstanceOf[TripleItem.Uri], minSupportUpper, y._2))
      val (tihi, thi) = TripleItemHashIndex.mapQuads(input.toDataset.quads.concat(newQuads))(TripleHashIndex(_))
      Index(thi, tihi)
    }
  }
}
package com.github.propi.rdfrules.ruleset.formats

import com.github.propi.rdfrules.algorithm.consumer.RuleIO
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.ruleset.formats.Json._
import com.github.propi.rdfrules.ruleset.{RulesetReader, RulesetSource, RulesetWriter}
import com.github.propi.rdfrules.utils.{ForEach, InputStreamBuilder, OutputStreamBuilder}
import spray.json._

import java.io._
import scala.io.Source
import scala.language.{implicitConversions, reflectiveCalls}

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
object NDJson {

  implicit def ndjsonRulesetWriter(source: RulesetSource.NDJson.type): RulesetWriter = (rules: ForEach[ResolvedRule], outputStreamBuilder: OutputStreamBuilder) => {
    val writer = new PrintWriter(new OutputStreamWriter(outputStreamBuilder.build, "UTF-8"))
    try {
      rules.map(rule => rule.toJson.compactPrint).foreach(writer.println)
    } finally {
      writer.close()
    }
  }

  implicit def ndjsonRulesetReader(source: RulesetSource.NDJson.type): RulesetReader = (inputStreamBuilder: InputStreamBuilder) => (f: ResolvedRule => Unit) => {
    val is = new BufferedInputStream(inputStreamBuilder.build)
    val source = Source.fromInputStream(is, "UTF-8")
    try {
      source.getLines().map(_.parseJson.convertTo[ResolvedRule]).foreach(f)
    } finally {
      source.close()
      is.close()
    }
  }

  private class NDJsonIO(file: File)(implicit mapper: TripleItemIndex) extends RuleIO {
    def writer[T](f: RuleIO.Writer => T): T = {
      val fos = new FileOutputStream(file)
      val writer = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"))
      try {
        f(new RuleIO.Writer {
          def write(rule: FinalRule): Unit = writer.println(ResolvedRule(rule).toJson.compactPrint)

          def flush(): Unit = {
            writer.flush()
            fos.getFD.sync()
          }
        })
      } finally {
        writer.close()
      }
    }

    def reader[T](f: RuleIO.Reader => T): T = {
      val is = new BufferedInputStream(new FileInputStream(file))
      val source = Source.fromInputStream(is, "UTF-8")
      try {
        val it = source.getLines().map(_.parseJson.convertTo[ResolvedRule])
        f(() => if (it.hasNext) Some(it.next().toRule) else None)
      } finally {
        source.close()
      }
    }
  }

  implicit def ndjsonIO(source: RulesetSource.NDJson.type)(implicit mapper: TripleItemIndex): File => RuleIO = new NDJsonIO(_)

}
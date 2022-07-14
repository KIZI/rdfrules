package com.github.propi.rdfrules.algorithm.consumer

import com.github.propi.rdfrules.algorithm.RuleConsumer
import com.github.propi.rdfrules.rule.Rule
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.utils.ForEach

import java.io.File
import java.util.concurrent.{ConcurrentLinkedQueue, LinkedBlockingQueue, TimeUnit}
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class InMemoryRuleConsumer private(prettyPrintedFile: Option[(File, File => PrettyPrintedWriter)]) extends RuleConsumer.NoEventRuleConsumer {

  private val rules = new ConcurrentLinkedQueue[FinalRule]
  private val isSavingToDisk = prettyPrintedFile.isDefined

  private lazy val messages = {
    val messages = new LinkedBlockingQueue[Option[FinalRule]]
    val job = new Runnable {
      def run(): Unit = {
        val prettyPrintedWriter = prettyPrintedFile.map(x => x._2(x._1)).get
        try {
          val syncDuration = 10 seconds
          var stopped = false
          var lastSync = System.currentTimeMillis()
          var isAdded = false
          while (!stopped) {
            messages.poll(syncDuration.toSeconds, TimeUnit.SECONDS) match {
              case null =>
              case Some(rule) =>
                isAdded = true
                prettyPrintedWriter.write(rule)
              case None => stopped = true
            }
            if (isAdded && System.currentTimeMillis > (lastSync + syncDuration.toMillis)) {
              prettyPrintedWriter.flush()
              lastSync = System.currentTimeMillis()
              isAdded = false
            }
          }
        } finally {
          prettyPrintedWriter.close()
        }
      }
    }
    val thread = new Thread(job)
    thread.start()
    messages
  }

  def send(rule: Rule): Unit = {
    val ruleSimple = Rule(rule)
    rules.add(ruleSimple)
    if (isSavingToDisk) {
      messages.put(Some(ruleSimple))
    }
  }

  def result: RuleConsumer.Result = {
    if (isSavingToDisk) {
      messages.put(None)
    }
    RuleConsumer.Result(new ForEach[FinalRule] {
      override lazy val knownSize: Int = rules.size()

      def foreach(f: FinalRule => Unit): Unit = rules.iterator().asScala.foreach(f)
    })
  }

}

object InMemoryRuleConsumer {

  def apply[T](f: InMemoryRuleConsumer => T): T = f(new InMemoryRuleConsumer(None))

  def apply[T](prettyPrintedFile: File, prettyPrintedWriterBuilder: File => PrettyPrintedWriter)(f: InMemoryRuleConsumer => T): T = {
    val x = new InMemoryRuleConsumer(Some(prettyPrintedFile -> prettyPrintedWriterBuilder))
    try {
      f(x)
    } finally {
      x.result
    }
  }

}
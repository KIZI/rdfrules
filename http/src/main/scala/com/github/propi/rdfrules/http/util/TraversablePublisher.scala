package com.github.propi.rdfrules.http.util

import com.github.propi.rdfrules.http.util.TraversablePublisher.ForeachThread
import com.github.propi.rdfrules.http.util.TraversablePublisher.Message.{Running, Stopping}
import com.github.propi.rdfrules.utils.ForEach
import com.typesafe.scalalogging.Logger
import org.reactivestreams.{Publisher, Subscriber, Subscription}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 14. 8. 2018.
  */
class TraversablePublisher[T] private(col: ForEach[T]) extends Publisher[T] {

  def subscribe(s: Subscriber[_ >: T]): Unit = {
    val foreachThread = new ForeachThread[T](col, s)
    new Thread(foreachThread).start()
    val subscription = new Subscription {
      def request(n: Long): Unit = foreachThread.request(n)

      def cancel(): Unit = foreachThread.cancel()
    }
    s.onSubscribe(subscription)
  }

}

object TraversablePublisher {

  private val logger = Logger(this.getClass)

  sealed trait Message {
    val x: Long

    def minusOne: Message
  }

  object Message {

    case class Running(x: Long) extends Message {
      def minusOne: Message = if (x <= 0) Running(0) else Running(x - 1)
    }

    case class Stopping(x: Long) extends Message {
      def minusOne: Message = if (x <= 0) Stopping(0) else Stopping(x - 1)
    }

  }

  class ForeachThread[T] private[TraversablePublisher](col: ForEach[T], s: Subscriber[_ >: T]) extends Runnable {

    private object Locker

    private var request: Message = Running(0)

    def request(x: Long): Unit = Locker.synchronized {
      request match {
        case Running(y) => request = Running(x + y)
        case _ =>
      }
      Locker.notify()
    }

    def cancel(): Unit = Locker.synchronized {
      request = Stopping(request.x)
      Locker.notify()
    }

    def run(): Unit = {
      var noRead = false
      try {
        col.foreach { el =>
          if (!noRead) {
            var stopped = false
            Locker.synchronized {
              while (!stopped) {
                if (request.x > 0) {
                  request = request.minusOne
                  stopped = true
                  s.onNext(el)
                } else if (request.isInstanceOf[Stopping]) {
                  noRead = true
                  stopped = true
                } else {
                  Locker.wait()
                }
              }
            }
          }
        }
        s.onComplete()
      } catch {
        case th: Throwable =>
          logger.error(th.getMessage, th)
          s.onError(th)
      }
    }

  }

  implicit def apply[T](col: ForEach[T]): Publisher[T] = new TraversablePublisher(col)

  implicit def apply[T](col: IterableOnce[T]): Publisher[T] = new TraversablePublisher(col)

}
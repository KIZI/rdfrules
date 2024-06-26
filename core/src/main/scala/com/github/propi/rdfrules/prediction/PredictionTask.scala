package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import com.github.propi.rdfrules.data.{TripleItem, TriplePosition}
import com.github.propi.rdfrules.index.IndexCollections.ReflexivableHashSet
import com.github.propi.rdfrules.index.IndexItem.{IntTriple, Triple}
import com.github.propi.rdfrules.index.{IndexCollections, TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.{Atom, TripleItemPosition}
import com.github.propi.rdfrules.data
import com.github.propi.rdfrules.serialization.TripleItemSerialization._
import com.github.propi.rdfrules.utils.serialization.{Deserializer, Serializer}

import java.io.ByteArrayInputStream

case class PredictionTask(p: Int, c: TripleItemPosition[Int]) {
  def targetVariable: ConceptPosition = c match {
    case _: TripleItemPosition.Subject[_] => TriplePosition.Object
    case _: TripleItemPosition.Object[_] => TriplePosition.Subject
  }

  def toTriple(x: Int): IntTriple = c match {
    case TripleItemPosition.Subject(s) => Triple(s, p, x)
    case TripleItemPosition.Object(o) => Triple(x, p, o)
  }

  def toAtom: Atom = c match {
    case TripleItemPosition.Subject(s) => Atom(Atom.Constant(s), p, Atom.Variable(0))
    case TripleItemPosition.Object(o) => Atom(Atom.Variable(0), p, Atom.Constant(o))
  }

  def predictionTaskPattern: PredictionTaskPattern.Mapped = PredictionTaskPattern.Mapped(Some(p), Some(targetVariable))

  def index(implicit tripleIndex: TripleIndex[Int]): ReflexivableHashSet[Int] = c match {
    case TripleItemPosition.Subject(s) => new ReflexivableHashSet[Int](s, tripleIndex.predicates.get(p).flatMap(_.subjects.get(s)).getOrElse(IndexCollections.emptySet[Int]))
    case TripleItemPosition.Object(o) => new ReflexivableHashSet[Int](o, tripleIndex.predicates.get(p).flatMap(_.objects.get(o)).getOrElse(IndexCollections.emptySet[Int]))
  }
}

object PredictionTask {
  case class Resolved(p: TripleItem.Uri, c: TripleItemPosition[TripleItem]) {
    override def toString: String = c match {
      case TripleItemPosition.Subject(s) => s"($s $p ?)"
      case TripleItemPosition.Object(o) => s"(? $p $o)"
    }

    def toTriple: data.Triple = c match {
      case TripleItemPosition.Subject(s) => data.Triple(s.asInstanceOf[TripleItem.Uri], p, TripleItem.Uri(""))
      case TripleItemPosition.Object(o) => data.Triple(TripleItem.Uri(""), p, o)
    }

    def mapped(implicit tripleItemIndex: TripleItemIndex): PredictionTask = PredictionTask(tripleItemIndex.getIndex(p), c.map(tripleItemIndex.getIndex))
  }

  object Resolved {
    def apply(predictionTask: PredictionTask)(implicit mapper: TripleItemIndex): Resolved = Resolved(
      mapper.getTripleItem(predictionTask.p).asInstanceOf[TripleItem.Uri],
      predictionTask.c.map(mapper.getTripleItem)
    )
  }

  def apply(triple: IntTriple)(implicit tripleIndex: TripleIndex[Int]): PredictionTask = tripleIndex.predicates.get(triple.p) match {
    case Some(pindex) => pindex.higherCardinalitySide match {
      case TriplePosition.Subject => PredictionTask(triple.p, TripleItemPosition.Subject(triple.s))
      case TriplePosition.Object => PredictionTask(triple.p, TripleItemPosition.Object(triple.o))
    }
    case None => PredictionTask(triple.p, TripleItemPosition.Subject(triple.s))
  }

  def apply(triple: IntTriple, targetVariable: ConceptPosition): PredictionTask = targetVariable match {
    case TriplePosition.Subject => PredictionTask(triple.p, TripleItemPosition.Object(triple.o))
    case TriplePosition.Object => PredictionTask(triple.p, TripleItemPosition.Subject(triple.s))
  }

  implicit def predictionTaskSerializer(implicit mapper: TripleItemIndex): Serializer[PredictionTask] = (v: PredictionTask) => {
    val resolved = Resolved(v)
    resolved.c match {
      case TripleItemPosition.Subject(s) => Serializer.serialize((1: Byte, s, resolved.p))
      case TripleItemPosition.Object(o) => Serializer.serialize((2: Byte, o, resolved.p))
    }
  }

  implicit def predictionTaskDeserializer(implicit mapper: TripleItemIndex): Deserializer[PredictionTask] = (v: Array[Byte]) => {
    val bais = new ByteArrayInputStream(v)
    val (t, c, p) = Deserializer.deserialize[(Byte, TripleItem, TripleItem.Uri)](bais)
    if (t == 1) {
      PredictionTask(mapper.getIndex(p), TripleItemPosition.Subject(mapper.getIndex(c)))
    } else {
      PredictionTask(mapper.getIndex(p), TripleItemPosition.Object(mapper.getIndex(c)))
    }
  }
}
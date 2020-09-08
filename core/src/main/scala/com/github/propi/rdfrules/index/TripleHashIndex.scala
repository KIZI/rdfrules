package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.index.TripleHashIndex._
import com.github.propi.rdfrules.index.TripleIndex.{HashMap, HashSet}
import com.github.propi.rdfrules.rule.TripleItemPosition
import com.github.propi.rdfrules.utils.Debugger

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
class TripleHashIndex[T] private(implicit collectionsBuilder: CollectionsBuilder[T]) extends TripleIndex[T] {

  private type ItemMap = MutableHashMap[T, MutableHashSet[T]]
  private type ItemMapWithGraphsAndSet = MutableHashMap[T, GraphsHashSet[MutableHashSet[T]]]
  private type ItemMapWithGraphsAndMap = MutableHashMap[T, GraphsHashSet[ItemMap]]

  private val _predicates: MutableHashMap[T, TriplePredicateIndex] = collectionsBuilder.emptyHashMap
  private val _subjects: MutableHashMap[T, TripleSubjectIndex] = collectionsBuilder.emptyHashMap
  private val _objects: MutableHashMap[T, TripleObjectIndex] = collectionsBuilder.emptyHashMap
  private val _sameAs = collection.mutable.ListBuffer.empty[(T, T)]

  private var graph: Option[T] = None
  private var severalGraphs: Boolean = false

  @volatile private var _size: Int = -1

  private class GraphsHashSet[C <: HashSet[T]](val value: C, val graphs: MutableHashSet[T]) extends HashSet[T] {
    def addGraph(g: T): Unit = graphs += g

    def iterator: Iterator[T] = value.iterator

    def contains(x: T): Boolean = value.contains(x)

    def size: Int = value.size

    def isEmpty: Boolean = value.isEmpty
  }

  private class TriplePredicateIndex(val subjects: ItemMapWithGraphsAndMap, val objects: ItemMapWithGraphsAndSet)(implicit collectionsBuilder: CollectionsBuilder[T]) extends PredicateIndex {
    @volatile private var _size: Int = -1
    @volatile private var _graphs: Option[HashSet[T]] = None

    def size: Int = {
      if (_size == -1) {
        _size = subjects.valuesIterator.map(_.size).sum
      }
      _size
    }

    def reset(): Unit = {
      _size = -1
      _graphs = None
    }

    //add all graphs to this predicate index - it is suitable for atom p(a, b) to enumerate all graphs
    //it is contructed from all predicate-subject graphs
    def graphs: HashSet[T] = _graphs match {
      case Some(x) => x
      case None =>
        val set = collectionsBuilder.emptySet
        subjects.valuesIterator.flatMap(_.graphs.iterator).foreach(set += _)
        set.trim()
        _graphs = Some(set)
        set
    }
  }

  private class TripleSubjectIndex(val objects: ItemMap, val predicates: MutableHashSet[T]) extends SubjectIndex {
    @volatile private var _size: Int = -1

    def reset(): Unit = _size = -1

    def size: Int = {
      if (_size == -1) {
        _size = objects.valuesIterator.map(_.size).sum
      }
      _size
    }
  }

  private class TripleObjectIndex(val predicates: MutableHashSet[T], computePredicateObjectSize: T => Int) extends ObjectIndex {
    @volatile private var _size: Int = -1

    def reset(): Unit = _size = -1

    def size: Int = {
      if (_size == -1) {
        _size = predicates.iterator.map(computePredicateObjectSize(_)).sum
      }
      _size
    }
  }

  def predicates: HashMap[T, PredicateIndex] = _predicates

  def subjects: HashMap[T, SubjectIndex] = _subjects

  def objects: HashMap[T, ObjectIndex] = _objects

  def quads: Iterator[IndexItem.Quad[T]] = {
    for {
      (p, m1) <- _predicates.pairIterator
      (s, m2) <- m1.subjects.pairIterator
      o <- m2.iterator
      g <- getGraphs(s, p, o).iterator
    } yield {
      IndexItem.Quad(s, p, o, g)
    }
  }

  def size: Int = {
    if (_size == -1) {
      _size = _predicates.valuesIterator.map(_.size).sum
    }
    _size
  }

  def reset(): Unit = {
    _size = -1
    _subjects.valuesIterator.foreach(_.reset())
    _predicates.valuesIterator.foreach(_.reset())
    _objects.valuesIterator.foreach(_.reset())
  }

  private def addQuadToSubjects(quad: IndexItem.Quad[T]): Unit = {
    val si = _subjects.getOrElseUpdate(quad.s, new TripleSubjectIndex(collectionsBuilder.emptyHashMap, collectionsBuilder.emptySet))
    si.predicates += quad.p
    si.objects.getOrElseUpdate(quad.o, collectionsBuilder.emptySet) += quad.p
  }

  private def addQuadToObjects(quad: IndexItem.Quad[T]): Unit = {
    val oi = _objects.getOrElseUpdate(quad.o, new TripleObjectIndex(collectionsBuilder.emptySet, p => _predicates(p).objects(quad.o).value.size))
    oi.predicates += quad.p
  }

  def evaluateAllLazyVals(): Unit = {
    size
    _subjects.valuesIterator.foreach(_.size)
    _objects.valuesIterator.foreach(_.size)
    if (graph.isEmpty) {
      _predicates.valuesIterator.foreach(_.graphs)
    }
  }

  def getGraphs(predicate: T): HashSet[T] = if (graph.nonEmpty) {
    val set = collectionsBuilder.emptySet
    set += graph.get
    set
  } else {
    _predicates(predicate).graphs
  }

  def getGraphs(predicate: T, tripleItemPosition: TripleItemPosition[T]): HashSet[T] = if (graph.nonEmpty) {
    val set = collectionsBuilder.emptySet
    set += graph.get
    set
  } else {
    val pi = _predicates(predicate)
    tripleItemPosition match {
      case TripleItemPosition.Subject(x) => pi.subjects(x).graphs
      case TripleItemPosition.Object(x) => pi.objects(x).graphs
      case _ => pi.graphs
    }
  }

  def getGraphs(subject: T, predicate: T, `object`: T): HashSet[T] = if (graph.nonEmpty) {
    val set = collectionsBuilder.emptySet
    set += graph.get
    set
  } else {
    _predicates(predicate).subjects(subject).value.apply(`object`)
  }

  private def emptyMapWithGraphs = new GraphsHashSet[ItemMap](collectionsBuilder.emptyHashMap, collectionsBuilder.emptySet)

  private def emptySetWithGraphs = new GraphsHashSet[MutableHashSet[T]](collectionsBuilder.emptySet, collectionsBuilder.emptySet)

  private def addGraph(quad: IndexItem.Quad[T]): Unit = {
    //get predicate index by a specific predicate
    val pi = _predicates.getOrElseUpdate(quad.p, new TriplePredicateIndex(collectionsBuilder.emptyHashMap, collectionsBuilder.emptyHashMap))
    //get predicate-subject index by a specific subject
    val psi = pi.subjects.getOrElseUpdate(quad.s, emptyMapWithGraphs)
    //add graph to this predicate-subject index - it is suitable for atom p(A, b) to enumerate all graphs
    psi.addGraph(quad.g)
    //get predicate-subject-object index by a specific object and add the graph
    // - it is suitable for enumerate all quads with graphs
    // - then construct Dataset from Index
    psi.value.getOrElseUpdate(quad.o, collectionsBuilder.emptySet) += quad.g
    //get predicate-object index by a specific object and add the graph - it is suitable for atom p(a, B) to enumerate all graphs
    pi.objects.getOrElseUpdate(quad.o, emptySetWithGraphs).addGraph(quad.g)
  }

  private def addSameAs(sameAs: IndexItem.SameAs[T]): Unit = {
    if (sameAs.s != sameAs.o) {
      _sameAs += (sameAs.o -> sameAs.s)
    }
  }

  private def resolveSameAs(implicit debugger: Debugger): Unit = {
    if (_sameAs.nonEmpty) {
      debugger.debug("SameAs resolving") { ad =>
        for {
          (p, pi) <- _predicates.pairIterator
          (replace, replacement) <- _sameAs.iterator
        } {
          for (si <- pi.subjects.get(replace)) {
            for (o <- si.iterator) {
              val graphs = getGraphs(replace, p, o).iterator.toList
              pi.objects.get(o).foreach(_.value -= replace)
              for (g <- graphs) {
                addQuad(IndexItem.Quad(replacement, p, o, g))
                ad.done()
              }
            }
            pi.subjects.remove(replace)
          }
          for (oi <- pi.objects.get(replace)) {
            for (s <- oi.iterator) {
              val graphs = getGraphs(s, p, replace).iterator.toList
              pi.subjects.get(s).foreach(_.value.remove(replace))
              for (g <- graphs) {
                addQuad(IndexItem.Quad(s, p, replacement, g))
                ad.done()
              }
            }
            pi.objects.remove(replace)
          }
        }
        for ((replace, replacement) <- _sameAs.iterator) {
          for {
            (s, o) <- _predicates
              .get(replace)
              .iterator
              .flatMap(_.subjects.pairIterator.flatMap(x => x._2.iterator.map(x._1 -> _)))
            g <- getGraphs(s, replace, o).iterator
          } {
            addQuad(IndexItem.Quad(s, replacement, o, g))
            ad.done()
          }
          _predicates.remove(replace)
        }
        _sameAs.clear()
      }
    }
  }

  private def addQuad(quad: IndexItem.Quad[T]): Unit = {
    if (graph.isEmpty) {
      if (severalGraphs) {
        addGraph(quad)
      } else {
        graph = Some(quad.g)
      }
    } else if (!graph.contains(quad.g)) {
      for {
        (p, m1) <- _predicates.pairIterator
        (o, m2) <- m1.objects.pairIterator
        s <- m2.iterator
        g <- graph
      } {
        addGraph(IndexItem.Quad(s, p, o, g))
      }
      addGraph(quad)
      severalGraphs = true
      graph = None
    }
    val pi = _predicates.getOrElseUpdate(quad.p, new TriplePredicateIndex(collectionsBuilder.emptyHashMap, collectionsBuilder.emptyHashMap))
    if (!severalGraphs) {
      pi.subjects
        .getOrElseUpdate(quad.s, emptyMapWithGraphs).value
        .getOrElseUpdate(quad.o, collectionsBuilder.emptySet)
    }
    pi.objects.getOrElseUpdate(quad.o, emptySetWithGraphs).value += quad.s
  }

  private def trimSubjects(): Unit = {
    for (x <- _subjects.valuesIterator) {
      for (x <- x.objects.valuesIterator) x.trim()
      x.predicates.trim()
      x.objects.trim()
    }
    _subjects.trim()
  }

  private def trimObjects(): Unit = {
    for (x <- _objects.valuesIterator) {
      x.predicates.trim()
    }
    _objects.trim()
  }

  private def trimPredicates(): Unit = {
    for (x <- _predicates.valuesIterator) {
      for (x <- x.subjects.valuesIterator) {
        x.value.trim()
        x.graphs.trim()
        for (x <- x.value.valuesIterator) x.trim()
      }
      for (x <- x.objects.valuesIterator) {
        x.value.trim()
        x.graphs.trim()
      }
      x.subjects.trim()
      x.objects.trim()
    }
    _predicates.trim()
  }

}

object TripleHashIndex {

  trait MutableHashSet[T] extends HashSet[T] {
    def +=(x: T): Unit

    def -=(x: T): Unit

    def trim(): Unit
  }

  trait MutableHashMap[K, V] extends HashMap[K, V] {
    def getOrElseUpdate(key: K, default: => V): V

    def remove(key: K): Unit

    def put(key: K, value: V): Unit

    def clear(): Unit

    def trim(): Unit
  }

  trait CollectionsBuilder[T] {
    def emptySet: MutableHashSet[T]

    def emptyHashMap[V]: MutableHashMap[T, V]
  }

  def addQuads[T](quads: Traversable[IndexItem[T]])(implicit thi: TripleHashIndex[T]): Unit = {
    try {
      quads.foreach {
        case quad: IndexItem.Quad[T] =>
          thi.addQuad(quad)
          thi.addQuadToSubjects(quad)
          thi.addQuadToObjects(quad)
        case sameAs: IndexItem.SameAs[T] => thi.addSameAs(sameAs)
        case _ =>
      }
    } finally {
      thi.resolveSameAs
    }
  }

  def apply[T](quads: Traversable[IndexItem[T]])(implicit debugger: Debugger, collectionsBuilder: CollectionsBuilder[T]): TripleHashIndex[T] = {
    val index = new TripleHashIndex[T]
    debugger.debug("Dataset indexing") { ad =>
      for (quad <- quads.view.takeWhile(_ => !debugger.isInterrupted)) {
        quad match {
          case quad: IndexItem.Quad[T] => index.addQuad(quad)
          case sameAs: IndexItem.SameAs[T] => index.addSameAs(sameAs)
          case _ =>
        }
        ad.done()
      }
    }
    if (debugger.isInterrupted) {
      debugger.logger.warn(s"The triple indexing task has been interrupted. The loaded index may not be complete.")
    }
    index.resolveSameAs
    debugger.logger.info("Predicates trimming.")
    index.trimPredicates()
    debugger.debug("Subjects indexing") { ad =>
      for (quad <- index.quads) {
        index.addQuadToSubjects(quad)
        ad.done()
      }
    }
    debugger.logger.info("Subjects trimming.")
    index.trimSubjects()
    debugger.debug("Objects indexing") { ad =>
      for (quad <- index.quads) {
        index.addQuadToObjects(quad)
        ad.done()
      }
    }
    debugger.logger.info("Objects trimming.")
    index.trimObjects()
    index
  }

}
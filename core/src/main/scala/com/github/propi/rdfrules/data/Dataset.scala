package com.github.propi.rdfrules.data

import java.io._

import com.github.propi.rdfrules.algorithm.RulesMining
import com.github.propi.rdfrules.data.Quad.QuadTraversableView
import com.github.propi.rdfrules.data.Triple.TripleTraversableView
import com.github.propi.rdfrules.data.ops._
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.index.Index.Mode
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.serialization.QuadSerialization._
import com.github.propi.rdfrules.utils.extensions.TraversableOnceExtension._
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}

/**
  * Created by Vaclav Zeman on 3. 10. 2017.
  */
class Dataset private(val quads: QuadTraversableView)
  extends Transformable[Quad, Dataset]
    with TriplesOps
    with QuadsOps[Dataset]
    with Discretizable[Dataset]
    with Cacheable[Quad, Dataset] {

  protected val serializer: Serializer[Quad] = implicitly[Serializer[Quad]]
  protected val deserializer: Deserializer[Quad] = implicitly[Deserializer[Quad]]
  protected val serializationSize: SerializationSize[Quad] = implicitly[SerializationSize[Quad]]

  protected def coll: Traversable[Quad] = quads

  protected def transform(col: Traversable[Quad]): Dataset = new Dataset(col.view)

  protected def transformQuads(col: Traversable[Quad]): Dataset = transform(col)

  def +(graph: Graph): Dataset = new Dataset(quads ++ graph.quads)

  def +(dataset: Dataset): Dataset = new Dataset(quads ++ dataset.quads)

  def triples: TripleTraversableView = quads.map(_.triple)

  def toGraphs: Traversable[Graph] = quads.map(_.graph).distinct.view.map(x => Graph(x, quads.filter(_.graph == x).map(_.triple)))

  def foreach(f: Quad => Unit): Unit = quads.foreach(f)

  def export(os: => OutputStream)(implicit writer: RdfWriter): Unit = writer.writeToOutputStream(this, os)

  def export(file: File)(implicit writer: RdfWriter): Unit = {
    val newWriter = if (writer == RdfWriter.NoWriter) RdfWriter(file) else writer
    export(new FileOutputStream(file))(newWriter)
  }

  def export(file: String)(implicit writer: RdfWriter): Unit = export(new File(file))

  def mine(miner: RulesMining): Ruleset = Index(this).mine(miner)

  def index(mode: Mode = Mode.PreservedInMemory): Index = Index(this, mode)

}

object Dataset {

  def apply(graph: Graph): Dataset = new Dataset(graph.quads)

  def apply(): Dataset = new Dataset(Traversable.empty[Quad].view)

  def apply(is: => InputStream)(implicit reader: RdfReader): Dataset = new Dataset(reader.fromInputStream(is))

  def apply(file: File)(implicit reader: RdfReader): Dataset = {
    val newReader = if (reader == RdfReader.NoReader) RdfReader(file) else reader
    new Dataset(newReader.fromFile(file))
  }

  def apply(file: String)(implicit reader: RdfReader): Dataset = apply(new File(file))

  def apply(quads: Traversable[Quad]): Dataset = new Dataset(quads.view)

  def fromCache(is: => InputStream): Dataset = new Dataset(
    new Traversable[Quad] {
      def foreach[U](f: Quad => U): Unit = {
        Deserializer.deserializeFromInputStream[Quad, Unit](is) { reader =>
          Stream.continually(reader.read()).takeWhile(_.isDefined).foreach(x => f(x.get))
        }
      }
    }.view
  )

  def fromCache(file: File): Dataset = fromCache(new FileInputStream(file))

  def fromCache(file: String): Dataset = fromCache(new File(file))

}
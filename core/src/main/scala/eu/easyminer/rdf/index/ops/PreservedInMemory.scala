package eu.easyminer.rdf.index.ops

import eu.easyminer.rdf.index.{Index, TripleHashIndex, TripleItemHashIndex}

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait PreservedInMemory extends Buildable {

  self: Index =>

  private lazy val thi = buildTripleHashIndex
  private lazy val tihi = buildTripleItemHashIndex

  def tripleMap[T](f: TripleHashIndex => T): T = f(thi)

  def tripleItemMap[T](f: TripleItemHashIndex => T): T = f(tihi)

}

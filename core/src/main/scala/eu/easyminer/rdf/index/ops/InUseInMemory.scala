package eu.easyminer.rdf.index.ops

import eu.easyminer.rdf.index.{Index, TripleHashIndex, TripleItemHashIndex}

/**
  * Created by Vaclav Zeman on 13. 3. 2018.
  */
trait InUseInMemory extends Buildable {

  self: Index =>

  def tripleMap[T](f: TripleHashIndex => T): T = f(buildTripleHashIndex)

  def tripleItemMap[T](f: TripleItemHashIndex => T): T = f(buildTripleItemHashIndex)

}

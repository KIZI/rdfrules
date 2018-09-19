package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.github.propi.rdfrules.gui.utils.StringConverters._

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class DropQuads(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.DropQuads
  val properties: Constants[Property] = Constants(
    new FixedText[Int]("value", "Drop first N quads", "10")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}
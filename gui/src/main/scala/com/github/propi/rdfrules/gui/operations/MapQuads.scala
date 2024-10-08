package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class MapQuads(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    Group("search", "Search")(implicit context => Constants(
      new OptionalText[String]("subject", "Subject", validator = RegExp("<.*>|.*:.*", true)),
      new OptionalText[String]("predicate", "Predicate", validator = RegExp("<.*>|.*:.*", true)),
      new OptionalText[String]("object", "Object"),
      new OptionalText[String]("graph", "Graph", validator = RegExp("<.*>|.*:.*", true)),
      new Checkbox("inverse", "Negation")
    )),
    Group("replacement", "Replacement")(implicit context => Constants(
      new OptionalText[String]("subject", "Subject", validator = RegExp("<.*>|_:.*", true)),
      new OptionalText[String]("predicate", "Predicate", validator = RegExp("<.*>|_:.*", true)),
      new OptionalText[String]("object", "Object"),
      new OptionalText[String]("graph", "Graph", validator = RegExp("<.*>|_:.*", true))
    ))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}
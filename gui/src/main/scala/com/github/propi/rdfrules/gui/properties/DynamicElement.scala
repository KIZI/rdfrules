package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.github.propi.rdfrules.gui.utils.ReactiveBinding
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.{Constant, Constants, Var}
import org.lrng.binding.html
import org.scalajs.dom.html.{Div, Span, TableRow}

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 17. 9. 2018.
  */
class DynamicElement(properties: Constants[Property], hidden: Boolean = false) extends Property {
  val name: String = properties.value.head.name
  val title: String = properties.value.head.title
  val summaryTitle: String = ""
  val descriptionVar: Var[String] = Var("")

  private val active: Var[Int] = Var(-1)

  if (hidden) isHidden.value = true

  for (i <- properties.value.indices) {
    properties.value(i).errorMsg.addListener((_: Option[String], newValue: Option[String]) => if (active.value == i) errorMsg.value = newValue)
  }

  def validate(): Option[String] = if (active.value >= 0) {
    properties.value(active.value).validate()
  } else {
    None
  }

  def setValue(data: js.Dynamic): Unit = {
    if (active.value >= 0) {
      properties.value(active.value).setValue(data)
    }
  }

  override def hasSummary: Binding[Boolean] = Binding.BindingInstances.bind(active)(x => if (x >= 0) properties.value(x).hasSummary else Constant(false))

  def summaryContentView: Binding[Span] = Binding.BindingInstances.bind(active)(x => if (x >= 0) properties.value(x).summaryView else ReactiveBinding.emptySpan)

  override def summaryView: Binding[Span] = summaryContentView

  @html
  def valueView: Binding[Div] = <div>
    {val activeIndex = active.bind
    if (activeIndex >= 0) {
      properties.value(activeIndex).valueView.bind
    } else {
      ReactiveBinding.empty.bind
    }}
  </div>

  override def view: Binding[TableRow] = super.view

  def setElement(x: Int): Unit = {
    active.value = x
    if (x < 0) {
      if (hidden) {
        isHidden.value = true
      }
      descriptionVar.value = ""
      errorMsg.value = None
    } else {
      val el = properties.value(x)
      descriptionVar.value = el.descriptionVar.value
      errorMsg.value = el.errorMsg.value
      if (hidden) {
        isHidden.value = false
      }
    }
  }

  def toJson: js.Any = if (active.value >= 0) properties.value(active.value).toJson else js.undefined
}

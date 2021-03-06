package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.HTMLInputElement

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class Checkbox(val name: String, val title: String, default: Boolean = false, description: String = "", onChecked: Boolean => Unit = _ => {}) extends Property {

  private var _isChecked: Boolean = default

  val descriptionVar: Binding.Var[String] = Var(description)

  def setValue(data: js.Dynamic): Unit = {
    _isChecked = data.asInstanceOf[Boolean]
    onChecked(_isChecked)
  }

  def isChecked: Boolean = _isChecked

  def validate(): Option[String] = None

  def toJson: js.Any = isChecked

  @dom
  final def valueView: Binding[Div] = {
    <div>
      <input type="checkbox" class="checkbox" checked={_isChecked} onchange={e: Event =>
      _isChecked = e.target.asInstanceOf[HTMLInputElement].checked
      onChecked(_isChecked)}/>
    </div>
  }

}
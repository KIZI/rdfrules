package com.github.propi.rdfrules.gui.properties

import com.github.propi.rdfrules.gui.Documentation.Context
import com.github.propi.rdfrules.gui.Property
import com.thoughtworks.binding.Binding.{Constants, Var, Vars}
import com.thoughtworks.binding.Binding
import org.lrng.binding.html
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 13. 9. 2018.
  */
class DynamicGroup private(val name: String, val title: String, properties: Context => Constants[Property])(implicit context: Context) extends Property {

  private val groups: Vars[Constants[Property]] = Vars.empty

  val descriptionVar: Binding.Var[String] = Var(context(title).description)

  def getGroups: Iterable[Constants[Property]] = groups.value

  def validate(): Option[String] = groups.value.iterator.flatMap(_.value.iterator).map(_.validate()).find(_.nonEmpty).flatten.map(x => s"There is an error within '$title' properties: $x")

  def setValue(data: js.Dynamic): Unit = {
    groups.value.clear()
    for (x <- data.asInstanceOf[js.Array[js.Dynamic]]) {
      val props = properties(context(title))
      for (prop <- props.value) {
        val propData = x.selectDynamic(prop.name)
        if (!js.isUndefined(propData)) prop.setValue(propData)
      }
      groups.value += props
    }
  }

  def toJson: js.Any = js.Array(groups.value.map(properties => js.Dictionary(properties.value.map(x => x.name -> x.toJson).filter(x => !js.isUndefined(x._2)).toList: _*)).toList: _*)

  @html
  def valueView: NodeBinding[Div] = {
    <div class="dynamic-group">
      {for (group <- groups) yield
      <table>
        <tr>
          <th colSpan={2}>
            <a class="del" onclick={_: Event => groups.value -= group}>
              <i class="material-icons">remove_circle_outline</i>
            </a>
          </th>
        </tr>{for (property <- group) yield {
        property.view.bind
      }}
      </table>}<a class="add" onclick={_: Event => groups.value += properties(context(title))}>
      <i class="material-icons">add_circle_outline</i>
    </a>
    </div>
  }

}

object DynamicGroup {

  def apply(name: String, title: String)(properties: Context => Constants[Property])(implicit context: Context): DynamicGroup = new DynamicGroup(name, title, properties)

}
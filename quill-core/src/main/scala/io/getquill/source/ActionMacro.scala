package io.getquill.source

import scala.reflect.macros.whitebox.Context

import io.getquill._
import io.getquill.ast._
import io.getquill.util.Messages.fail

trait ActionMacro extends EncodingMacro {
  this: SourceMacro =>

  val c: Context
  import c.universe.{ Ident => _, _ }

  def runAction[S, T](action: Ast, params: List[(Ident, Type)])(implicit s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree =
    params match {
      case Nil =>
        q"${c.prefix}.execute(${prepare(action, params.map(_._1))}._1)"

      case List((param, tpe)) if (t.tpe <:< c.weakTypeOf[UnassignedAction[Any]]) =>
        val encodingValue = encoding(param, Encoding.inferEncoder[S](c))(c.WeakTypeTag(tpe))
        val bindings = bindingMap(encodingValue)
        val idents = bindings.keys.toList
        val assignedAction = AssignedAction(action, idents.map(k => Assignment(k.name, k)))
        val encodedParams = EncodeParams.raw[S](c)(bindings)
        expandedTree(assignedAction, idents.toList, List(tpe), encodedParams)

      case params =>
        val encodedParams = EncodeParams[S](c)(bindingMap(params))
        expandedTree(action, params.map(_._1), params.map(_._2), encodedParams)
    }

  private def expandedTree(action: Ast, idents: List[Ident], paramsTypes: List[Type], encodedParams: Tree) =
    q"""
    {
      val (sql, bindings: List[io.getquill.ast.Ident]) =
        ${prepare(action, idents)}

      (values: List[(..$paramsTypes)]) =>
        ${c.prefix}.execute(
          sql,
          values.map(value => $encodedParams(bindings.map(_.name))))
    }
    """

  private def bindingMap(value: Value, option: Boolean = false): collection.Map[Ident, (Tree, Tree)] =
    value match {
      case CaseClassValue(tpe, values) =>
        values.flatten.map(bindingMap(_)).flatten.toMap
      case OptionValue(value) =>
        bindingMap(value, option = true)
      case SimpleValue(ast, encoder, optionEncoder) =>
        val (ident, tree) = bindingTree(ast)
        val enc = if (option) optionEncoder else encoder
        collection.Map((ident, (enc, tree)))
    }

  private def bindingTree(ast: Ast): (Ident, Tree) =
    ast match {
      case Property(ast: Property, name) =>
        val (ident, tree) = bindingTree(ast)
        (ident, q"$tree.${TermName(name)}")
      case Property(ast: Ident, name) =>
        (Ident(name), q"value.${TermName(name)}")
      case other =>
        fail(s"Can't bind '$other'.")
    }

  private def bindingMap(params: List[(Ident, Type)]): collection.Map[Ident, (Type, Tree)] =
    params match {
      case (param, tpe) :: Nil =>
        collection.Map((param, (tpe, q"value")))
      case params =>
        (for (((param, tpe), index) <- params.zipWithIndex) yield {
          (param, (tpe, q"value.${TermName(s"_${index + 1}")}"))
        }).toMap
    }
}

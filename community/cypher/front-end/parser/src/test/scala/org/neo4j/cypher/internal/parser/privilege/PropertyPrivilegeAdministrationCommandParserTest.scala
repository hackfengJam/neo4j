/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 * This file is a commercial add-on to Neo4j Enterprise Edition.
 */
package org.neo4j.cypher.internal.parser.privilege

import org.neo4j.cypher.internal.ast
import org.neo4j.cypher.internal.ast.AllPropertyResource
import org.neo4j.cypher.internal.ast.PrivilegeType
import org.neo4j.cypher.internal.ast.SetPropertyAction
import org.neo4j.cypher.internal.parser.AdministrationCommandParserTestBase
import org.neo4j.cypher.internal.util.InputPosition

class PropertyPrivilegeAdministrationCommandParserTest extends AdministrationCommandParserTestBase {

  type privilegeTypeFunction = () => InputPosition => PrivilegeType

  Seq(
    ("GRANT", "TO", grant: resourcePrivilegeFunc),
    ("DENY", "TO", deny: resourcePrivilegeFunc),
    ("REVOKE GRANT", "FROM", revokeGrant: resourcePrivilegeFunc),
    ("REVOKE DENY", "FROM", revokeDeny: resourcePrivilegeFunc),
    ("REVOKE", "FROM", revokeBoth: resourcePrivilegeFunc)
  ).foreach {
    case (verb: String, preposition: String, func: resourcePrivilegeFunc) =>

          test(s"$verb SET PROPERTY { prop } ON GRAPH foo $preposition role") {
            yields(func(ast.GraphPrivilege(SetPropertyAction)(_), ast.PropertiesResource(Seq("prop"))(_), List(ast.NamedGraphScope(literal("foo"))(_)), ast.ElementsAllQualifier()(_), Seq(literal("role"))))
          }

          // Multiple properties should be allowed

          test(s"$verb SET PROPERTY { * } ON GRAPH foo $preposition role") {
            yields(func(ast.GraphPrivilege(SetPropertyAction)(_), AllPropertyResource()(_), List(ast.NamedGraphScope(literal("foo"))(_)), ast.ElementsAllQualifier()(_), Seq(literal("role"))))
          }

          test(s"$verb SET PROPERTY { prop1, prop2 } ON GRAPH foo $preposition role") {
            yields(func(ast.GraphPrivilege(SetPropertyAction)(_), ast.PropertiesResource(Seq("prop1", "prop2"))(_), List(ast.NamedGraphScope(literal("foo"))(_)), ast.ElementsAllQualifier()(_), Seq(literal("role"))))
          }

          // Multiple graphs should be allowed

          test(s"$verb SET PROPERTY { prop } ON GRAPHS * $preposition role") {
            yields(func(ast.GraphPrivilege(SetPropertyAction)(_), ast.PropertiesResource(Seq("prop"))(_), List(ast.AllGraphsScope()(_)), ast.ElementsAllQualifier()(_), Seq(literal("role"))))
          }

          test(s"$verb SET PROPERTY { prop } ON GRAPHS foo,bar $preposition role") {
            yields(func(ast.GraphPrivilege(SetPropertyAction)(_), ast.PropertiesResource(Seq("prop"))(_), List(ast.NamedGraphScope(literal("foo"))(_), ast.NamedGraphScope(literal("bar"))(_)), ast.ElementsAllQualifier()(_), Seq(literal("role"))))
          }

          // Qualifiers

          test(s"$verb SET PROPERTY { prop } ON GRAPHS foo ELEMENTS foo,bar $preposition role") {
            yields(func(ast.GraphPrivilege(SetPropertyAction)(_), ast.PropertiesResource(Seq("prop"))(_), List(ast.NamedGraphScope(literal("foo"))(_)), ast.ElementsQualifier(List("foo", "bar"))(_), Seq(literal("role"))))
          }

          test(s"$verb SET PROPERTY { prop } ON GRAPHS foo NODES foo,bar $preposition role") {
            yields(func(ast.GraphPrivilege(SetPropertyAction)(_), ast.PropertiesResource(Seq("prop"))(_), List(ast.NamedGraphScope(literal("foo"))(_)), ast.LabelsQualifier(List("foo", "bar"))(_), Seq(literal("role"))))
          }

          test(s"$verb SET PROPERTY { prop } ON GRAPHS foo NODES * $preposition role") {
            yields(func(ast.GraphPrivilege(SetPropertyAction)(_), ast.PropertiesResource(Seq("prop"))(_), List(ast.NamedGraphScope(literal("foo"))(_)), ast.LabelAllQualifier()(_), Seq(literal("role"))))
          }

          test(s"$verb SET PROPERTY { prop } ON GRAPHS foo RELATIONSHIPS foo,bar $preposition role") {
            yields(func(ast.GraphPrivilege(SetPropertyAction)(_), ast.PropertiesResource(Seq("prop"))(_), List(ast.NamedGraphScope(literal("foo"))(_)), ast.RelationshipsQualifier(List("foo", "bar"))(_), Seq(literal("role"))))
          }

          test(s"$verb SET PROPERTY { prop } ON GRAPHS foo RELATIONSHIPS * $preposition role") {
            yields(func(ast.GraphPrivilege(SetPropertyAction)(_), ast.PropertiesResource(Seq("prop"))(_), List(ast.NamedGraphScope(literal("foo"))(_)), ast.RelationshipAllQualifier()(_), Seq(literal("role"))))
          }

          // Multiple roles should be allowed
          test(s"$verb SET PROPERTY { prop } ON GRAPHS foo $preposition role1, role2") {
            yields(func(ast.GraphPrivilege(SetPropertyAction)(_), ast.PropertiesResource(Seq("prop"))(_), List(ast.NamedGraphScope(literal("foo"))(_)), ast.ElementsAllQualifier()(_), Seq(literal("role1"), literal("role2"))))
          }

          // Parameter values

          test(s"$verb SET PROPERTY { prop } ON GRAPH $$foo $preposition role") {
            yields(func(ast.GraphPrivilege(SetPropertyAction)(_), ast.PropertiesResource(Seq("prop"))(_), List(ast.NamedGraphScope(param("foo"))(_)), ast.ElementsAllQualifier()(_), Seq(literal("role"))))
          }

          test(s"$verb SET PROPERTY { prop } ON GRAPH foo $preposition $$role") {
            yields(func(ast.GraphPrivilege(SetPropertyAction)(_), ast.PropertiesResource(Seq("prop"))(_), List(ast.NamedGraphScope(literal("foo"))(_)), ast.ElementsAllQualifier()(_), Seq(param("role"))))
          }

          // PROPERTYS instead of LABEL
          test(s"$verb SET PROPERTYS { prop } ON GRAPH * $preposition role") {
            failsToParse
          }

          // Database instead of graph keyword

          test(s"$verb SET PROPERTY { prop } ON DATABASES * $preposition role") {
            failsToParse
          }

          test(s"$verb SET PROPERTY { prop } ON DATABASE foo $preposition role") {
            failsToParse
          }

          test(s"$verb SET PROPERTY { prop } ON DEFAULT DATABASE $preposition role") {
            failsToParse
          }
      }

}

package io.getquill.source.jdbc.mysql

import io.getquill.source.jdbc.JdbcSource
import io.getquill.naming.Literal
import io.getquill.source.sql.idiom.MySQLDialect

object testMysqlDB extends JdbcSource[MySQLDialect, Literal]

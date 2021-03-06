package io.getquill.source.sql.idiom

import io.getquill._
import io.getquill.naming.Literal
import io.getquill.source.sql.SqlQuery
import io.getquill.util.Show._

class NullsFirstClauseSpec extends Spec {

  val subject = new SqlIdiom with NullsOrderingClause {
    def prepare(sql: String) = sql
  }

  implicit val naming = new Literal {}

  import subject._

  "adds the nulls ordering clause" - {
    "asc" in {
      val q = quote {
        qr1.sortBy(t => t.s).map(t => t.s)
      }
      SqlQuery(q.ast).show mustEqual "SELECT t.s FROM TestEntity t ORDER BY t.s NULLS FIRST"
    }
    "desc" in {
      val q = quote {
        qr1.sortBy(t => t.s).reverse.map(t => t.s)
      }
      SqlQuery(q.ast).show mustEqual "SELECT t.s FROM TestEntity t ORDER BY t.s DESC NULLS LAST"
    }
    "mixed" in {
      val q = quote {
        qr1.sortBy(t => t.i).sortBy(t => t.s).reverse.map(t => t.s)
      }
      SqlQuery(q.ast).show mustEqual "SELECT t.s FROM (SELECT t.* FROM TestEntity t ORDER BY t.i NULLS FIRST) t ORDER BY t.s DESC NULLS LAST"
    }
  }
}

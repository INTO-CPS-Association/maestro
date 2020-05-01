package org.intocps.initializer

import scala.annotation.tailrec

object Conversions {
  //     At this stage all the independent FMU and instance statements have been created.
  //     The next step is getting and setting inputs and outputs according to the topological sorting

  def sequence[A](a: Seq[Option[A]]) : Option[Seq[A]] = {
    @tailrec
    def iterate(seq: Seq[Option[A]], acc: Seq[A]) : Option[Seq[A]] = seq match {
      case Nil => Some(acc.reverse)
      case x :: xs => x match {
        case None => None
        case Some(value: A) => iterate(xs, value +: acc)
      }
    }
    iterate(a, Seq.empty)
  }

}

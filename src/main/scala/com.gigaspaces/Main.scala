package com.gigaspaces

import java.io.IOException

import zio.{DefaultRuntime, IO, Schedule, Task, UIO, ZIO}

import scala.util.Try
import scala.concurrent.Future
import scala.io.StdIn

object Main extends App {
  val s1: UIO[Int] = ZIO.succeed(42)
  val s2: Task[Int] = Task.succeed(42)

  lazy val bigList = (0 to 1000000).toList
  lazy val bigString = bigList.map(_.toString).mkString("\n")

  val s3 = ZIO.effectTotal(bigString)
  val f1 = ZIO.fail("Uh oh!")
  val f2 = Task.fail(new Exception("Uh oh!"))

  val zoption: ZIO[Any, Unit, Int] = ZIO.fromOption(Some(2))
  val zoption2: ZIO[Any, String, Int] = zoption.mapError(_ => "It wasn't there!")

  val zeither = ZIO.fromEither(Right("Success!"))

  val ztry = ZIO.fromTry(Try(42 / 0))

  val zfun: ZIO[Int, Nothing, Int] =
    ZIO.fromFunction((i: Int) => i * i)


  lazy val future = Future.successful("Hello!")

  val zfuture: Task[String] =
    ZIO.fromFuture { implicit ec =>
      future.map(_ => "Goodbye!")
    }

  val getStrLn: Task[Unit] =
    ZIO.effect(StdIn.readLine())

  def putStrLn(line: String): UIO[Unit] =
    ZIO.effectTotal(println(line))

  val getStrLn2: IO[IOException, String] =
    ZIO.effect(StdIn.readLine()).refineToOrDie[IOException]

  import zio.blocking._
  val sleeping =
    effectBlocking(Thread.sleep(Long.MaxValue))


  val succeded: UIO[Int] = IO.succeed(21).map(_ * 2)


  val failed: IO[Exception, Unit] =
    IO.fail("No no!").mapError(msg => new Exception(msg))

  val sequenced =
    getStrLn.flatMap(input => putStrLn(s"You entered: $input"))


  val program =
    for {
      _    <- putStrLn("Hello! What is your name?")
      name <- getStrLn
      _    <- putStrLn(s"Hello, $name, welcome to ZIO!")
    } yield ()

  val zipped: UIO[(String, Int)] =
    ZIO.succeed("4").zip(ZIO.succeed(2))

  val zeither1: UIO[Either[String, Int]] =
    IO.fail("Uh oh!").either

  val finalizer =
    UIO.effectTotal(println("Finalizing!"))

  val finalized: IO[String, Unit] =
    IO.fail("Failed!").ensuring(finalizer)

  val runtime = new DefaultRuntime {}
//  runtime.unsafeRun(putStrLn("Hello World!"))
//  runtime.unsafeRun(finalized)

  def fib(n: Long): UIO[Long] = UIO {
    if (n <= 1) UIO.succeed(n)
    else fib(n - 1).zipWith(fib(n - 2))(_ + _)
  }.flatten

  val fib100Fiber: ZIO[Any, Nothing, Long] =
    for {
      fiber <- fib(10).fork
      v <- fiber.join
    } yield v
  println(runtime.unsafeRun(fib100Fiber))


  println(runtime.unsafeRun(for {
    winner <- IO.succeed("Hello").race(IO.succeed("Goodbye"))
  } yield winner
  ))

  import zio.duration._
  runtime.unsafeRun(IO.succeed("Hello").timeout(10.seconds).repeat(Schedule.once))
}
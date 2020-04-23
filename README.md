## Project initialized from sbt template

[flink-project.g8](https://github.com/tillrohrmann/flink-project.g8)

Using flink 1.7.2 to maintain compatibility with [Stream Processing with Apache Flink](http://shop.oreilly.com/product/0636920057321.do)

Project initialization command:
```
sbt new tillrohrmann/flink-project.g8 \
  --name=sbt-flink172 \
  --organization=com.mitzit \
  --version=0.1 \
  --flink_version=1.7.2 \
  --scala=2.12.11
```

in order to run a single class use the provided 'mainRunner' intended for use with Intellij

`sbt "mainRunner/runMain com.mitzit.WordCount"`

## Original README from template
A Flink application project using Scala and SBT.

To run and test your application locally, you can just execute `sbt run` then select the main class that contains the Flink job .

You can also package the application into a fat jar with `sbt assembly`, then submit it as usual, with something like:

```
flink run -c org.example.WordCount /path/to/your/project/my-app/target/scala-2.11/testme-assembly-0.1-SNAPSHOT.jar
```


You can also run your application from within IntelliJ:  select the classpath of the 'mainRunner' module in the run/debug configurations.
Simply open 'Run -> Edit configurations...' and then select 'mainRunner' from the "Use classpath of module" dropbox.

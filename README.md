# jConstraints-smtinterpol #

*jConstraints-smtinterpol* is a plugin for 
[jConstraints][0], adding SMTInterpol as a 
constraint solver.

## Dependencies ##

* [SMTInterpol][2] (LGPL v3)

SMTInterpol is not distributed along with 
*jconstraints-smtinterpol*, but is available 
for download at [Uni Freiburg][4]


## Building and Installing ##

* Download [SMTInterpol][4]
* You must install the `smtinterpol.jar`.

```bash
# mvn install:install-file -Dfile=smtinterpol.jar -DgroupId=de.uni_freiburg.informatik.ultimate -DartifactId=smtinterpol -Dversion=2.0 -Dpackaging=jar
```
* In the *jConstraints-smtinterpol* folder, run `mvn install`
* If the compilation was successful, the *jConstraints-smtinterpol*
  plugin can be found in the JAR file
  `target/jconstraints-smtinterpol-[VERSION].jar`
   

[0]: https://github.com/psycopaths/jconstraints
[2]: http://ultimate.informatik.uni-freiburg.de/smtinterpol/
[4]: http://ultimate.informatik.uni-freiburg.de/smtinterpol/download.html


#!/bin/sh

java -Xmx64m -server \
  -cp target/classes:target/test-classes:lib/\* \
   -Xrunhprof:cpu=samples,depth=10,verbose=n,interval=2 \
$*

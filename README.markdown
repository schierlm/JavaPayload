## JavaPayload

JavaPayload is a collection of pure Java payloads to be used for post-exploitation from pure Java exploits or from common misconfigurations (like not password protected Tomcat manager or debugger port).

Outdated documentation can be found at 

+ http://schierlm.users.sourceforge.net/JavaPayload/
+ http://schierlm.users.sourceforge.net/J2EEPayload/
+ http://schierlm.users.sourceforge.net/SpawnJavaPayload/

and inside this repo.

The current home of this project is http://javapayload.sourceforge.net/ and its source is at http://javapayload.svn.sourceforge.net/.

## Compiling

JavaPayload tries to compile its class files for the oldest Java version that is able to run the particular exploit/payload. After Java 8, the compiler stopped supporting compilation for Java VMs older than Java 6. Therefore compilation will fail. Either use Java 8 (or older) for compilation, or patch the various `source` and `target` versions in `build.xml` to read at least 1.6.

## Required libraries

Since Git does not perform well with huge libraries inside the repository, the required libraries
have been left out. They are available as a separate download at

http://github.com/downloads/schierlm/JavaPayload/LibsBundle.zip

## Get in touch

You can contact me at schierlm@users.sourceforge.net. Or, just fork and send pull requests. I will try my very best to get them merged into SVN (still a git newbie), or may even switch over to GitHub if contributions get overwhelming :-)

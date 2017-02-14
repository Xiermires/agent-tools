#agent-tools
A collection of tools to allow local & remote loading of agents into the JVM. 

--------
Overview
--------

It provides 12 methods :

(local version) -> Works on the local JVM.

* load (ClassFileTransformer ctf) -> loads a ClassFileTransformer
* remove (ClassFileTransformer ctf) -> removes a ClassFileTransformer
* reset (String... classNames) -> loads the original version of the class (w/o any bytecode modifications)
* retransform (ClassFileTransformer ctf, String... classNames) -> retransforms the classes using ctf
* redefine(String... classNames) -> redefines the classes using the currently loaded ctfs
* getObjectSize(Object obj) -> returns the size of the object in bytes

(remote version) -> Same as local, but working on a remote JVM. 

* load (int pid, ClassFileTransformer ctf) -> see above
* remove (int pid, ClassFileTransformer ctf) -> see above
* reset (int pid, String... classNames) -> see above
* retransform (int pid, ClassFileTransformer see above
* redefine(int pid, String... classNames) -> see above
* loadJar(int pid, String jarName, byte[] jarBytes) -> loads the .jar in the remote VM class path 

---------
Specifics 
---------

Local :

AgentTools loads itself in the running JVM as .jar. 
Because everything needed is in CP already, the generated .jar contains the needed MANIFEST.MF only.

Remote :

AgentTools creates a .jar with the required classes and loads into the alien VM. 
Inside the alien VM, the agent.jar creates a MBean which facades the functionality.
Back in the initial VM, we JMX proxy the MBean to access the functionality.

---------
Examples 
---------

Check test classes. Here below some quick example.

```java

class Dog {
		void bark() {} // silent dog
	}

class Example {
	public static void Main(String...)
	{
		final ClassFileTransformer ctf = new Onomatopoeia("woof-woof", "Dog"); // This transformer changes the Dog class to console out the given onomatopoeia
		AgentTools.add(ctf);
		new Dog().bark() // prints woof-woof
		
		AgentTools.reset("Dog");
		new Dog().bark() // silence
		
		AgentTools.retransform(ctf, "Dog");
		new Dog().bark() // prints woof-woof
		
		AgentTools.remove(ctf);
		new Dog().bark() // still woof-woof
		
		AgentTools.add(new Onomatopoeia("bup-bup", "Dog"));
		AgentTools.redefine("Dog");
		new Dog().bark() // prints bup-bup. The dog just learned Catalan !		
	}
}
```

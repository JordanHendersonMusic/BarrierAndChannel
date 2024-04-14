# A Supercollider Quark

A thread synchronisation barrier and a channel.

Barrier will wait until n number of threads have finished.

Channel deals with consumers and produces.


```Supercollider
r = Routine.run {
	var b  = Barrier.collect(
		{ var v = 1.0.rand; v.wait; v},
		{ var v = 1.0.rand; v.wait; v},
		{ var v = 1.0.rand; v.wait; v},
		{ var v = 1.0.rand; v.wait; v}
	);
	\waiting.postln;
	b.value.postln;
	\done.postln;
}
```

```Supercollider
~ch = Channel();

fork { loop {
  var t = 1.0.rand;
  t.wait;
  ~ch.push(t);
}};

fork { loop { ~ch.pop().debug(\got) }};
```

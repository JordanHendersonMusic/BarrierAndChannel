# A Supercollider Quark

A thread synchronisation barrier and a channel.

Barrier will wait until n number of threads have finished.

Channel deals with consumers and produces.


```Supercollider
r = Routine {
	var barrier = Barrier.forkAll(
		{1.0.rand.wait; \a.postln},
		{1.0.rand.wait; \b.postln},
		{1.0.rand.wait; \c.postln},
		{1.0.rand.wait; \d.postln}
	);

	\waiting.postln;
	barrier.wait;
	\done.postln
};

r.play;

r = Routine {
	var func = {|n| 1.0.rand.wait; n.postln};
	var barrier = Barrier.forkN(f, 10 );

	\waiting.postln;
	barrier.wait;
	\done.postln
};

r.play
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

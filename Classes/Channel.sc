ChannelClosedError : Error {}
Channel {
	var buffer;
	var awaitingThreads;
	var is_open;

	// modes, defines what the pop count value means
	*exactly { ^\exactly }
	*moreThan { ^\moreThan }
	*lessThan { ^\lessThan }

	*new {  ^super.newCopyArgs(LinkedList(), LinkedList(), true) }

	push {|value, close=false|
		var t;
		if(is_open.not){ Error("Channel has been closed").throw };
		is_open = close.not;
		buffer.add(value);

		if(awaitingThreads.any({|t| t.isGreedy })) {
			awaitingThreads.do({|t, n|
				case
				{t[\mode] == Channel.exactly} {
					if(buffer.size >= t[\count]){
						awaitingThreads.remove(t);
						t[\thread].clock.sched(0, t[\thread]);
						^this;
					}
				}
				{t[\mode] == Channel.lessThan} {
					awaitingThreads.remove(t);
					t[\thread].clock.sched(0, t[\thread]);
					^this;
				}
				{t[\mode] == Channel.moreThan} {
					if(buffer.size > t[\count]){
						awaitingThreads.remove(t);
						t[\thread].clock.sched(0, t[\thread]);
						^this;
					}
				}
			})
		} {
			awaitingThreads.first !? {|t|
				case
				{t[\mode] == Channel.exactly} {
					if(buffer.size >= t[\count]){
						awaitingThreads.remove(t);
						t[\thread].clock.sched(0, t[\thread])
					}
				}
				{t[\mode] == Channel.lessThan} {
					awaitingThreads.remove(t);
					t[\thread].clock.sched(0, t[\thread])
				}
				{t[\mode] == Channel.moreThan} {
					if(buffer.size > t[\count]){
						awaitingThreads.remove(t);
						t[\thread].clock.sched(0, t[\thread])
					}
				}
			}
		}
	}

	close {
		is_open = false;
	}

	holds { ^buffer.size }

	empty { this.holds() == 0 }

	spillDo {|f|
		buffer.size.do{ f.( buffer.popFirst ) };
	}
	spillCollect {|f|
		^buffer.size.collect{ f.( buffer.popFirst ) };
	}

	doUntilClosed {|f, timeout = 10|
		try {
			f.(this.pop(count: 1, timeout: timeout, mode: Channel.exactly))
		} {|er|
			if(er.isKindOf(ChannelClosedError)){
				^nil
			} {
				er.throw
			}
		};
		while{is_open or: (buffer.size > 0)}{
			f.(this.pop(count: 1, timeout: timeout, mode: Channel.exactly))
		}
	}

	pop {|count=1, timeout=10, mode=(Channel.exactly), isGreedy=false|
		var is_timeout = false;
		var has_returned = false;
		var this_thread = thisThread;

		var try_return = {
			case
			{ mode == Channel.exactly }{
				if (buffer.size >= count) {
					has_returned = true;
					if(count == 1){
						^buffer.popFirst
					} {
						^count.collect({buffer.popFirst})
					}
				};
			}
			{ mode == Channel.lessThan }{
				if (buffer.size > 0) {
					has_returned = true;
					if(buffer.size == 1){
						^[buffer.popFirst]
					} {
						^buffer.size.clip(1, count).collect({buffer.popFirst})
					}
				};
			}
			{ mode == Channel.moreThan }{
				if (buffer.size > count) {
					has_returned = true;
					if(buffer.size == 1){
						^[buffer.popFirst]
					} {
						^buffer.size.collect({buffer.popFirst})
					}
				};
			}
		};

		if(is_open.not and: (buffer.size == 0)){
			ChannelClosedError("Channel has been closed").throw
		};

		try_return.();

		awaitingThreads.add((
			\thread: this_thread,
			\count: count,
			\mode: mode,
			\isGreedy: isGreedy
		));

		fork {
			timeout.wait;
			is_timeout = true;
			if(has_returned.not){
				this_thread.clock.sched(0, this_thread)
			}
		};

		\awaiting.yield;

		if (is_timeout) { Error("timeout").throw };

		try_return.();
		// should not happen
		Error("was awoken without providing a value").throw;
	}
}

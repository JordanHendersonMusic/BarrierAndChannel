Barrier {
	var countDown;
	var condVar;
	var <is_finished;  // only public member
	// only used for collect case
	var expects_value;
	var held_value;

	*do { |...seriesOfFuncs|
		^super.newCopyArgs(
			seriesOfFuncs.size, // countDown
			CondVar(), // condVar
			false, // is_finished
			false, //  expects_value
			nil // held_value
		)
		.pr_do(seriesOfFuncs);
	}

	*collect { |...seriesOfFuncs|
		^super.newCopyArgs(
			seriesOfFuncs.size, // countDown
			CondVar(), // condVar
			false, // is_finished
			true, //  expects_value
			nil.dup(seriesOfFuncs.size) // held_value
		)
		.pr_collect(seriesOfFuncs, 1);
	}

	*doNTimes { |n, function| ^Barrier.do(*{function}.dup(n)) }

	*collectNTimes { |n, function| ^Barrier.collect(*{function}.dup(n)) }

	wait {
		expects_value.not.if(
			{ this.prTryWait() },
			{ ("Barrier.collect(N) returns a value, "
				+ "call 'value' instead, or use Barrier.do(N) "
				+ "when no value is needed").error }
		)
	}

	value {
		^expects_value.not.if(
			{ "Barrier.do(N) does not return a value, call 'wait' instead, or use Barrier.collect(N)".error },
			{ this.prTryValue()	}
		)
	}

	// private:
	prTryWait {
		^try {
			condVar.wait{is_finished} // normal path
		} {|er|
			if((er.class == PrimitiveFailedError) && (er.failedPrimitiveName == '_RoutineYield'),
				{  "Barrier.wait must be called inside a Routine/Thread".throw	},
				{er.throw} // the function being called raised an error
			);
		}

	}

	prTryValue {
		^try {
			if(is_finished,
				{held_value}, // already computed
				{ condVar.wait{is_finished}; held_value } // not finished yet, try waiting.
			)
		}
		{ |er|
			if((er.class == PrimitiveFailedError) && (er.failedPrimitiveName == '_RoutineYield'),
				{  ("Barrier's value has not completed, "
					+ "either call 'value' in a Routine/Thread, "
					+ "or, literally wait until the proccess has finished and call value again").throw
				},
				{er.throw} // the function being called raised an error
			);
		}
	}

	prRelease {
		countDown = countDown - 1;
		condVar.signalOne;
		if(countDown <= 0, {is_finished = true })
	}

	pr_collect { |funcs, clump|
		funcs.do{ |f, n|
			fork { held_value[n] = f.(n); this.prRelease()}
		};
		^this
	}

	pr_do { |funcs, clump|
		funcs.do{ |f, n|
			fork { f.(n); this.prRelease() }
		};
		^this
	}
}

























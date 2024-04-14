Barrier {
	var countDown;
	var condVar;
	var <isFinished;

	// only used for collect case
	var expects_value;
	var held_value;

	*do { |...seriesOfFuncs|
		^super.newCopyArgs(
			seriesOfFuncs.size, // countDown
			CondVar(), // condVar
			false, // isFinished
			false, //  expects_value
			nil // held_value
		)
		.pr_do(seriesOfFuncs);
	}

	*collect { |...seriesOfFuncs|
		^super.newCopyArgs(
			seriesOfFuncs.size, // countDown
			CondVar(), // condVar
			false, // isFinished
			true, //  expects_value
			nil.dup(seriesOfFuncs.size) // held_value
		)
		.pr_collect(seriesOfFuncs, 1);
	}

	*doNTimes { |n, function| ^Barrier.do(*{function}.dup(n)) }

	*collectNTimes { |n, function| ^Barrier.collect(*{function}.dup(n)) }

	wait { this.prTryWait() }

	value {
		^expects_value.not.if(
			{ "Barrier.do(N) does not return a value, call 'wait' instead, or use Barrier.collect(N)".error },
			{ this.prTryValue()	}
		)
	}
	await { ^this.value }

	loopWhileExecuting {|func|
		var count = 0;
		var should_loop = true;

		fork{
			while
			{isFinished.not && should_loop}
			{
				var wait_time = func.(count, this);
				if( wait_time.isKindOf(Number),
					{ wait_time.wait },
					{
						should_loop = false;
						format(
							"loopWhileExecuting's function MUST return a number that will be 'waited' "
							+ "before the function is called again, expected Number, received %",
							wait_time.class
						).err;
					}
				);
				count = count + 1;
			}
		};
		^this;
	}


	// private:
	prTryWait {
		^try {
			condVar.wait{isFinished} // normal path
		} {|er|
			if((er.class == PrimitiveFailedError) && (er.failedPrimitiveName == '_RoutineYield'),
				{  "Barrier.wait must be called inside a Routine/Thread".throw	},
				{er.throw} // the function being called raised an error
			);
		}

	}

	prTryValue {
		^try {
			if(isFinished,
				{held_value}, // already computed
				{ condVar.wait{isFinished}; held_value } // not finished yet, try waiting.
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
		if(countDown <= 0, {isFinished = true })
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

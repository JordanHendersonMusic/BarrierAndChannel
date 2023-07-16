Channel {
	var array;
	var condVar;

	*new { ^super.newCopyArgs([], CondVar()) }

	insert { |v|
		array = array ++ v;
		condVar.signalAll;
	}

	insertAll {|...v|
		array = array ++ v;
		condVar.signalAll;
	}

	insertResultOf {|function|
		fork {
			this.insert(function.());
		}
	}

	extract {
		^if(array.size > 0,
			{
				var out = array[0];
				array = array[1..];
				out
			},
			{
				this.prTryWait();
				this.extract();
			}
		)
	}

	extractAll {
		^if(array.size > 0,
			{
				var out = array;
				array = [];
				out
			},
			{
				this.prTryWait();
				this.extractAll();
			}
		)
	}

	has_value { ^array.size > 0 }

	prTryWait {
		try {
			condVar.wait{array.size > 0}
		} { |er|
			if((er.class == PrimitiveFailedError) && (er.failedPrimitiveName == '_RoutineYield'),
				{  "Channel.wait must be called inside a Routine".throw	},
				{er.throw} // something else has happened, perhaps impossible?
			);
		}
	}
}
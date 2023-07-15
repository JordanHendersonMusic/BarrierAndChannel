Barrier {
	var countDown;
	var condVar;

	*forkAll { |...arrayOfFuncs|
		var self = super.newCopyArgs(
			arrayOfFuncs.size,
			CondVar()
		);

		arrayOfFuncs.do({|f|
			fork {
				f.();
				self.prRelease()
			}
		});
		^self;
	}

	*forkN { |func, n|
		var self = super.newCopyArgs(
			n.asInteger,
			CondVar()
		);

		n.do({|count|
			fork {
				func.(count);
				self.prRelease()
			}
		});
		^self
	}

	wait {
		condVar.wait{countDown <= 0};
	}

	prRelease {
		countDown = countDown - 1;
		condVar.signalOne;
	}

}
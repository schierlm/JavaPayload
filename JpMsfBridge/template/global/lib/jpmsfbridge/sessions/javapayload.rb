require 'msf/base'
require 'msf/base/sessions/command_shell'


module JpMsfBridge
module Sessions

class JavaPayload < Msf::Sessions::CommandShell

	#
	# Returns the type of session.
	#
	def self.type
		"JavaPayload"
	end
	
	#
	# Initializes the session.
	#
	def initialize(rstream, opts={})
		super(nil)
		@tmp_rstream = rstream
	end
	
	def do_wrap() 
		self.rstream = wrap(@tmp_rstream)
		self.ring    = Rex::IO::RingBuffer.new(self.rstream, {:size => 100 })
	end
	
	def wrap(rstream)
		server = Rex::Socket.create_tcp_server('LocalPort' => 0)
		port = server.getsockname[2]
		pid = Process.spawn("#{JpMsfBridge::Config::JavaExecutable} -classpath #{JpMsfBridge::Config::ClassPath} jpmsfbridge.stage.StageBridge #{port} -- #{stagecommand}")
		Process.detach pid
		sock_msf = server.accept
		sock_stager = server.accept
		server.close()

		Rex::ThreadFactory.spawn('JavaPayload', false) {
			buf1 = sock_stager.read(4096)
			while buf1 and buf1 != ""
				rstream.put(buf1)
				buf1 = sock_stager.read(4096)
			end
			rstream.close()
		}
		Rex::ThreadFactory.spawn('JavaPayload', false) {
			buf2 = rstream.read(4096)
			while buf2 and buf2 != ""
				sock_stager.put(buf2)
				buf2 = rstream.read(4096)
			end
			sock_stager.close()
		}
		sock_msf
	end

	#
	# Returns the session description.
	#
	def desc
		"JavaPayload"
	end
	
	attr_accessor :stagecommand
end
end
end
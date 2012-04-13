module JpMsfBridge
module Handler

module JavaPayload

	include Msf::Handler::ReverseTcp

	def initialize(info = {})
		super(info)

		deregister_options('LHOST','LPORT')

		self.handler_queue = ::Queue.new
	end

	def setup_handler
		self.listener_sock = Rex::Socket.create_tcp_server('LocalPort' => 0)
		port = self.listener_sock.getsockname[2]
		pid = Process.spawn("#{JpMsfBridge::Config::JavaExecutable} -classpath #{JpMsfBridge::Config::ClassPath} jpmsfbridge.stager.StagerBridge #{cmdline} -- LocalProxy ReverseTCP localhost #{port}")
		Process.detach pid
		print_status("Started JavaPayload Proxy handler on port #{port}")
	end
end
end
end

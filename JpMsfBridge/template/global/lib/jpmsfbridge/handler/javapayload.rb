module JpMsfBridge
module Handler

module DynstagerFunctions

	def with_dynstagers(name)
$$#foreach($dynstager in ${globals.dynstagers})
$$#if($dynstager.parameters.size() == 1)
$$		datastore["DYNSTAGER_${globals.toLowerUnderscores(${dynstager.name}).toUpperCase()}"].split(' ').each do |arg|
$$			name = "${dynstager.name}_#{name} #{arg}"
		end
$$#elseif($dynstager.extraArg)
$$		datastore["DYNSTAGER_${globals.toLowerUnderscores(${dynstager.name}).toUpperCase()}"].split(' ').each do |arg|
$$			name = "${dynstager.name}$#{arg}_#{name}"
		end
$$#else
$$		datastore["DYNSTAGER_${globals.toLowerUnderscores(${dynstager.name}).toUpperCase()}"].times do
$$			name = "${dynstager.name}_#{name}"
		end
$$#end
$$#end
		name
	end

end

module DynstagerSupport

	def initialize(info = {})
		super(info)

		register_options(
			[
$$#foreach($dynstager in ${globals.dynstagers})
$$#if($foreach.hasNext)
$$#set($comma=",")
$$#else
$$#set($comma="")
$$#end
$$#if($dynstager.parameters.size() == 1)
$$				Msf::OptString.new('DYNSTAGER_${globals.toLowerUnderscores(${dynstager.name}).toUpperCase()}', [false, 'Arguments for ${dynstager.name} dynstager', ''])${comma}
$$#elseif($dynstager.extraArg)
$$				Msf::OptString.new('DYNSTAGER_${globals.toLowerUnderscores(${dynstager.name}).toUpperCase()}', [false, 'Arguments for ${dynstager.name} dynstager', ''])${comma}
$$#else
$$				Msf::OptInt.new('DYNSTAGER_${globals.toLowerUnderscores(${dynstager.name}).toUpperCase()}', [true, 'Number of invocations of ${dynstager.name} dynstager', 0])${comma}
$$#end
$$#end
			], self.class)
	end

	include JpMsfBridge::Handler::DynstagerFunctions

end

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
	
	include JpMsfBridge::Handler::DynstagerFunctions

end
end
end

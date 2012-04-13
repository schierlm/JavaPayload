$$output	javapayload_${globals.toLowerUnderscores(${info.name})}.rb
require 'msf/core'
require 'msf/core/payload/java'
require "#{File.expand_path(File.dirname(__FILE__))}/../../../../lib/jpmsfbridge/config"
$$require "#{JpMsfBridge::Config::Root}/sessions/javapayload"

module Metasploit3

	# The stager should have already included this
	#include Msf::Payload::Java

	def initialize(info = {})
		super(update_info(info,
$$			'Name'          => 'JavaPayload ${info.name} Stage',
			'Version'       => '$Revision$',
			'Description'   => %q{
$$				Run ${info.name} from JavaPayload.
				
$$				${info.module.summary}.
				
$$				${globals.indent(${info.module.description}, 4)}
			},
			'Author'        => [ 'mihi' ],
			'Platform'      => 'java',
			'Arch'          => ARCH_JAVA,
			'PayloadCompat' =>
				{
					'Convention' => 'javasocket',
				},
			'License'       => MSF_LICENSE,
			'Session'       => JpMsfBridge::Sessions::JavaPayload))

$$#if (${info.parameters.size()} > 0)
		register_options(
			[
$$#foreach($param in ${info.parameters})
$$#if($foreach.hasNext)
$$#set($comma=",")
$$#else
$$#set($comma="")
$$#end
$$				${param.rubyType}.new('${param.parameter.name}', [${param.rubyMandatory}, '${param.parameter.summary}'])${comma}
$$#end
			], self.class)
$$#end

		register_advanced_options(
			[
				OptString.new('InitialAutoRunScript', [false, "An initial script to run on session creation (before AutoRunScript)", '']),
				OptString.new('AutoRunScript', [false, "A script to run automatically on session creation.", ''])
			], self.class)
	end

	def on_session(session)
		super

		# Configure input/output to match the payload
		session.user_input  = self.user_input if self.user_input
		session.user_output = self.user_output if self.user_output
		if self.platform and self.platform.kind_of? Msf::Module::PlatformList
			session.platform = self.platform.platforms.first.realname.downcase
		end
		if self.platform and self.platform.kind_of? Msf::Module::Platform
			session.platform = self.platform.realname.downcase
		end
		session.arch = self.arch if self.arch

$$		session.stagecommand = "${info.name}" +
$$#foreach($param in ${info.parameters})
$$		" #{datastore['${param.parameter.name}']}" +
$$#end
		""

		session.do_wrap()
	end
	

	#
	# Override the Payload::Java version so we can offload the staging to JpMsfBridge
	#
	def generate_stage
		""
	end
end
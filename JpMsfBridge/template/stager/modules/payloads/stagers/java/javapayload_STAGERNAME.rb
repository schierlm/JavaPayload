$$output	javapayload_${globals.toLowerUnderscores(${info.name})}.rb
require 'msf/core'
require "#{File.expand_path(File.dirname(__FILE__))}/../../../../lib/jpmsfbridge/config"
$$require "#{JpMsfBridge::Config::Root}/handler/javapayload"

module Metasploit3

	include Msf::Payload::Stager
	include Msf::Payload::Java
	
	include JpMsfBridge::Handler::DynstagerSupport

	def initialize(info = {})
		super(merge_info(info,
$$			'Name'          => 'JavaPayload ${info.name} Stager',
			'Version'       => '$Revision$',
			'Description'   => %q{
$$				Run ${info.name} from JavaPayload.
				
$$				${info.module.summary}.
				
$$				${globals.indent(${info.module.description}, 4)}
			},
			'Author'        => [ 'mihi' ],
			'License'       => MSF_LICENSE,
			'Platform'      => 'java',
			'Arch'          => ARCH_JAVA,
$$			'Handler'       => JpMsfBridge::Handler::JavaPayload,
			'Convention'    => 'javasocket',
			'Stager'        => {'Payload' => ""}
			))

$$#if (${info.parameters.size()} > 0)
		register_options(
			[
				Msf::OptString.new('JAVAPAYLOAD_CRYPTER', [false, 'Crypter to use for dynamically generated classes', '']),
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

		@class_files = [ ]
	end

	# payload_set.recalculate will call this for every stager/stage combination. So hardcode this to
	# a realistic approximate value...
	def size
		7500
	end

	def self.handler_type_alias
$$		"javapayload_${globals.toLowerUnderscores(${info.name})}"
	end
 
	def connection_type
$$		"${info.connectionType}"
	end

	def config
		crypter = ""
		if datastore['JAVAPAYLOAD_CRYPTER']
			crypter = "-Djavapayload.crypter=#{datastore['JAVAPAYLOAD_CRYPTER']} "
		end
		pp = IO.popen("#{JpMsfBridge::Config::JavaExecutable} -classpath #{JpMsfBridge::Config::ClassPath} #{crypter}jpmsfbridge.stager.StagerEncoder #{cmdline}")
		c = pp.read
		pp.close
		c
	end
	
	def cmdline
$$		with_dynstagers("${info.name}") +
$$#foreach($param in ${info.parameters})
$$		" #{datastore['${param.parameter.name}']}" +
$$#end
		""
	end
end

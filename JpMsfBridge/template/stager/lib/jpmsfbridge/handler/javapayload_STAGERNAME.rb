$$output	javapayload_${globals.toLowerUnderscores(${info.name})}.rb
require "#{JpMsfBridge::Config::Root}/handler/javapayload"

module JpMsfBridge
module Handler

$$module JavaPayload_${info.name}

	include JpMsfBridge::Handler::JavaPayload

	def self.handler_type
$$		"javapayload_${globals.toLowerUnderscores(${info.name})}"
	end

	def self.general_handler_type
$$		"${info.connectionType}"
	end
	
	def cmdline
$$		"${info.name}" +
$$#foreach($param in ${info.parameters})
$$		" #{datastore['${param.parameter.name}']}" +
$$#end
		""
	end
end
end
end

# The prelude in 1.9.1 injects rubygems.rb into $LOADED_FEATURES
# which prevents the `require 'rubygems'` from actually loading
# the site's version of rubygems. So we have to use it's API
# to get it's prelude out of the way.
#
if RUBY_VERSION =~ /^1\.9\.1/ && defined?(Gem::QuickLoader)
  Gem::QuickLoader.load_full_rubygems_library
end

require 'rubygems'
require 'rubygems/gem_runner'
require 'rubygems/exceptions'

begin
  Gem::GemRunner.new.run %W[install bundler --install-dir #{ENV['GEM_PATH']}]
rescue Gem::SystemExitException => e
  # don't exit yet!
end

gem 'bundler', '>= 0'

require 'bundler'

require 'bundler/friendly_errors'
Bundler.with_friendly_errors do
  require 'bundler/cli'
  Bundler::CLI.start %W[install --path=#{ENV['GEM_PATH']}], :debug => true
end

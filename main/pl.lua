init = function(args)
   wrk.init(args)
   depth = tonumber(args[2]) or 1
   local r = {}
   for i=1,depth do
      r[i] = wrk.format(nil, args[1] or "/")
   end
   req = table.concat(r)
end

request = function()
   return req
end

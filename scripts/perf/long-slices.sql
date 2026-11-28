-- Main-thread slices longer than 8 ms (one 120 Hz frame) in the Tsuzuki process.
select s.ts, round(s.dur / 1e6, 1) as ms, s.depth, s.name
from slice s join thread_track tt on s.track_id = tt.id join thread t using (utid) join process p using (upid)
where p.name like 'io.github.manojx2005.tsuzuki%' and t.tid = p.pid and s.dur > 8e6
order by s.ts limit 80;

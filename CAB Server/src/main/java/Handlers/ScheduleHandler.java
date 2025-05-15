package Handlers;

import Scheduler.ScheduleGenerator;
import Scheduler.ScheduleGenerator.Result;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.*;

public class ScheduleHandler implements Route {

  private static final Moshi MOSHI = new Moshi.Builder().build();
  private static final JsonAdapter<Map<String,Object>> JSON =
      MOSHI.adapter(Types.newParameterizedType(Map.class, String.class, Object.class));

  @Override
  public Object handle(Request req, Response res) throws Exception {
    List<String> errors = new ArrayList<>();

    ScheduleErrorChecker.Params p = ScheduleErrorChecker.parseParams(req, errors);
    if (!errors.isEmpty()) {
      return JSON.toJson(Map.of("success", false, "errors", errors));
    }

    ScheduleErrorChecker.validatePreFilter(p, errors);
    if (!errors.isEmpty()) {
      return JSON.toJson(Map.of("success", false, "errors", errors));
    }

    ScheduleGenerator gen = new ScheduleGenerator(p.classesPerSemester, p.coursesTaken,
        p.remainingRequired, p.necessaryCourses, p.availableTimes, p.dayAvailability,
        p.balance, p.requiredThisSem, p.preferredDepts, p.needWRIT);

    gen.loadCourseData(p.term);
    gen.filterCourses(p.term);

    ScheduleErrorChecker.validatePostFilter(gen, p, errors);
    if (!errors.isEmpty()) {
      return JSON.toJson(Map.of("success", false, "errors", errors));
    }

    if (p.needWRIT && gen.getFilteredCourses().stream().noneMatch(c -> Boolean.TRUE.equals(c.get("writ")))) {
      return JSON.toJson(Map.of(
          "success", false,
          "errors", List.of("No WRIT‑designated course fits the given constraints")
      ));
    }

    Result result = gen.generateSchedules(p.term);
    return JSON.toJson(buildResponse(result));
  }

  @SuppressWarnings("unchecked")
  private static Map<String,Object> buildResponse(Result result) {
    Map<String,Object> outPut = new LinkedHashMap<>();
    outPut.put("success", result.errors.isEmpty());
    outPut.put("errors", result.errors);
    outPut.put("schedulesCount", result.schedules.size());

    List<Map<String,Object>> schedOut = new ArrayList<>();
    for (var schedule : result.schedules) {
      Map<String,Object> map = new LinkedHashMap<>();
      map.put("score", schedule.score);
      List<Map<String,Object>> cm = new ArrayList<>();
      for (var c : schedule.courses) {
        cm.add(Map.of(
            "code",  c.get("code"),
            "title", c.get("title"),
            "meets", c.get("meets"),
            "writ",  c.get("writ")
        ));
      }
      map.put("courses", cm);
      schedOut.add(map);
    }
    outPut.put("schedules", schedOut);
    return outPut;
  }
}
package com.octopus.demo.admin.controller;

import com.octopus.demo.admin.annotation.OperLog;
import com.octopus.demo.admin.dto.JobRequest;
import com.octopus.demo.admin.entity.SysJob;
import com.octopus.demo.admin.entity.SysJobLog;
import com.octopus.demo.admin.service.JobLogService;
import com.octopus.demo.admin.service.JobService;
import com.octopus.demo.admin.vo.JobLogVO;
import com.octopus.demo.admin.vo.JobVO;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.common.bean.R;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final JobLogService jobLogService;

    public JobController(JobService jobService, JobLogService jobLogService) {
        this.jobService = jobService;
        this.jobLogService = jobLogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('perm:sys:job:list')")
    public R<PageResultBean<JobVO>> findAll(PageQueryBean query,
                                            @RequestParam(required = false) String jobName,
                                            @RequestParam(required = false) Integer status) {
        PageResultBean<SysJob> result = jobService.findAll(query, jobName, status);
        PageResultBean<JobVO> voResult = new PageResultBean<>();
        voResult.setCount(result.getCount());
        voResult.setList(result.getList().stream().map(JobVO::from).collect(Collectors.toList()));
        return R.ok(voResult);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('perm:sys:job:add')")
    @OperLog(title = "新增任务", module = "job")
    public R<JobVO> create(@RequestBody JobRequest request) {
        return R.ok(JobVO.from(jobService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:sys:job:edit')")
    @OperLog(title = "修改任务", module = "job")
    public R<JobVO> update(@PathVariable Long id, @RequestBody JobRequest request) {
        return R.ok(JobVO.from(jobService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('perm:sys:job:delete')")
    @OperLog(title = "删除任务", module = "job")
    public R<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAuthority('perm:sys:job:edit')")
    @OperLog(title = "暂停任务", module = "job")
    public R<JobVO> pause(@PathVariable Long id) {
        return R.ok(JobVO.from(jobService.pause(id)));
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('perm:sys:job:edit')")
    @OperLog(title = "恢复任务", module = "job")
    public R<JobVO> resume(@PathVariable Long id) {
        return R.ok(JobVO.from(jobService.resume(id)));
    }

    @PostMapping("/{id}/run")
    @PreAuthorize("hasAuthority('perm:sys:job:run')")
    @OperLog(title = "立即执行任务", module = "job")
    public R<JobVO> run(@PathVariable Long id) {
        return R.ok(JobVO.from(jobService.run(id)));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('perm:sys:job:list')")
    public R<PageResultBean<JobLogVO>> logs(PageQueryBean query,
                                            @RequestParam(required = false) Long jobId,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                            @RequestParam(required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        PageResultBean<SysJobLog> result = jobLogService.findAll(query, jobId, status, from, to);
        PageResultBean<JobLogVO> voResult = new PageResultBean<>();
        voResult.setCount(result.getCount());
        voResult.setList(result.getList().stream().map(JobLogVO::from).collect(Collectors.toList()));
        return R.ok(voResult);
    }
}

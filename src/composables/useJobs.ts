import { ref } from "vue";
import { jobs as staticJobs, type Job } from "../data/jobs";
import { apiUrl } from "../utils/api";

type ApiJob = {
  slug:string; title:string; category:Job["category"]; businessUnit:string;
  requiredLocation:string; workMode:string; salaryRange?:string; summary:string;
  responsibilities:string[]; requirements:string[]; bonus:string[];
};

const jobs = ref<Job[]>([...staticJobs]);

const mapJob = (job:ApiJob):Job => ({
  slug:job.slug,
  title:job.title,
  category:job.category,
  unit:job.businessUnit,
  location:`${job.requiredLocation} · ${job.workMode}`,
  compensation:job.salaryRange || undefined,
  summary:job.summary,
  duties:job.responsibilities,
  requirements:job.requirements,
  bonus:job.bonus,
});

export function useJobs(){
  async function loadJobs(){
    try{
      const response=await fetch(apiUrl("/api/jobs"));
      if(!response.ok)return;
      const remote=await response.json() as ApiJob[];
      jobs.value=remote.map(mapJob);
    }catch{/* Local frontend remains usable while the Java API is offline. */}
  }
  async function loadJob(slug:string){
    const existing=jobs.value.find(job=>job.slug===slug);
    if(existing)return existing;
    try{
      const response=await fetch(apiUrl(`/api/jobs/${encodeURIComponent(slug)}`));
      if(!response.ok)return undefined;
      const job=mapJob(await response.json() as ApiJob);
      jobs.value.push(job);
      return job;
    }catch{return undefined}
  }
  return{jobs,loadJobs,loadJob};
}

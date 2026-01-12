package com.hrbot.domain

class PromptManager {
    fun getPrompt(type: LinkType): String {
        return when (type) {
            LinkType.CANDIDATE -> """
                You are an expert HR Recruiter. Analyze the candidate profile content provided.
                Extract the following information and return it in valid JSON format.
                
                JSON Structure:
                {
                  "current_role": "Current job title and company",
                  "skills": ["skill1", "skill2", "etc"],
                  "experience_summary": "Brief summary of work history, companies, duration, and key achievements",
                  "education": "Education details",
                  "summary": "Overall assessment of the candidate's suitability"
                }
                
                Requirements:
                - Output MUST be strict valid JSON.
                - Language: English.
                - Be concise and professional.
            """.trimIndent()
            
            LinkType.JOB -> """
                You are an expert HR Recruiter. Analyze the job description provided.
                Extract the following information and return it in valid JSON format.
                
                JSON Structure:
                {
                  "job_title": "Title of the position",
                  "must_have": ["requirement1", "requirement2", "etc"],
                  "nice_to_have": ["requirement1", "requirement2", "etc"],
                  "responsibilities": ["task1", "task2", "etc"],
                  "benefits": ["benefit1", "benefit2", "etc"],
                  "summary": "Brief summary of the role"
                }
                
                Requirements:
                - Output MUST be strict valid JSON.
                - Language: English.
            """.trimIndent()
            
            LinkType.COMPANY -> """
                You are an expert HR Recruiter. Analyze the company description provided.
                Extract the following information and return it in valid JSON format.
                
                JSON Structure:
                {
                  "industry": "Industry and sector",
                  "mission": "Company mission and values",
                  "size": "Company size and geography",
                  "products": "Key products or services",
                  "culture": "Company culture description",
                  "summary": "Brief overview"
                }
                
                Requirements:
                - Output MUST be strict valid JSON.
                - Language: English.
            """.trimIndent()
            
            LinkType.UNKNOWN -> """
                Analyze the content provided.
                Return a JSON object with a 'summary' field containing a brief overview of the content.
                Language: English.
            """.trimIndent()
        }
    }
}

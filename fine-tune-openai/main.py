import os
import time
import openai

def main():
    # Retrieve API key from environment
    openai.api_key = os.getenv("OPENAI_API_KEY")
    if not openai.api_key:
        raise EnvironmentError("Please set the OPENAI_API_KEY environment variable.")

    # Upload the training file
    training_file = openai.File.create(
        file=open("yahtzee_training_data.jsonl", "rb"),
        purpose="fine-tune"
    )
    print(f"Training file uploaded. ID: {training_file.id}")

    # Create the fine-tuning job
    job = openai.FineTuningJob.create(
        training_file=training_file.id,
        model="gpt-3.5-turbo"
    )
    print(f"Fine-tuning job created. ID: {job.id}")

    # Poll the job status until it finishes
    while True:
        status_response = openai.FineTuningJob.retrieve(job.id)
        status = status_response.status
        print(f"Job status: {status}")
        if status == "succeeded":
            print("Fine-tuning succeeded.")
            break
        elif status == "failed":
            print("Fine-tuning failed.")
            break
        else:
            # Wait before polling again
            time.sleep(10)

    # List all fine-tuning jobs
    all_jobs = openai.FineTuningJob.list()
    print("Current fine-tuning jobs:")
    print(all_jobs)

if __name__ == "__main__":
    main()

FROM ml-workspace-lab:latest

# Add service code to image
COPY docker-res/service /workspace/service
ENV EXECUTE_CODE=/workspace/service

# Install requirements only
RUN python /resources/scripts/execute_code.py --requirements-only

# Execute only the code at container startup
CMD ["python", "/resources/run.py", "--code-only"]
